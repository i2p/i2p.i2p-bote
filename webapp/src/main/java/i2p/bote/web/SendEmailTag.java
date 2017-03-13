/**
 * Copyright (C) 2009  HungryHobo@mail.i2p
 * 
 * The GPG fingerprint for HungryHobo@mail.i2p is:
 * 6DD3 EAA2 9990 29BC 4AD2 7486 1E2C 7B61 76DC DC12
 * 
 * This file is part of I2P-Bote.
 * I2P-Bote is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * I2P-Bote is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with I2P-Bote.  If not, see <http://www.gnu.org/licenses/>.
 */

package i2p.bote.web;

import i2p.bote.I2PBote;
import i2p.bote.email.Attachment;
import i2p.bote.email.Email;
import i2p.bote.email.FileAttachment;
import i2p.bote.email.NoIdentityForSenderException;
import i2p.bote.fileencryption.PasswordException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import net.i2p.util.Log;

import static i2p.bote.web.WebappUtil._t;

public class SendEmailTag extends BodyTagSupport {
    private static final long serialVersionUID = 5746062176954959787L;
    
    private Log log = new Log(SendEmailTag.class);
    private String senderAddress;
    private List<Recipient> recipients = new ArrayList<Recipient>();
    private List<Attachment> attachments = new ArrayList<Attachment>();
    private String subject;
    private String message;
    private boolean includeSentTime;
    
    /**
     * Overridden to remove parameters from the previous SendEmailTag object so the old
     * parameters aren't used for the new SendEmailTag (at least Tomcat re-uses SendEmailTag objects)
     */
    @Override
    public int doStartTag() throws JspException {
        recipients.clear();
        attachments.clear();
        return super.doStartTag();
    }
    
    @Override
    public int doEndTag() throws JspException {
        JspWriter out = pageContext.getOut();

        Email email = new Email(includeSentTime);
        String statusMessage;
        if (recipients.isEmpty())
            statusMessage = _t("Error: Please add at least one recipient.");
        else
            try {
                // set addresses
                InternetAddress ia = new InternetAddress(senderAddress);
                email.setFrom(ia);
                // We must continue to set "Sender:" even with only one mailbox
                // in "From:", which is against RFC 2822 but required for older
                // Bote versions to see a sender (and validate the signature).
                email.setSender(ia);
                email.setSubject(subject, "UTF-8");
                for (Recipient recipient: recipients)
                    email.addRecipient(recipient.type, recipient.address);
                // TODO: Comment out until we determine if this is necessary
                //email.fixAddresses();
                
                // set the text and add attachments
                email.setContent(message, attachments);
                
                // send the email
                I2PBote.getInstance().sendEmail(email);
                
                // delete attachment temp files
                for (Attachment attachment: attachments) {
                    if (!attachment.clean())
                        log.error("Can't clean up attachment: <" + attachment + ">");
                }
                
                statusMessage = _t("The email has been queued for sending.");
            }
            catch (PasswordException e) {
                throw new JspException(e);
            }
            catch (NoIdentityForSenderException e) {
                statusMessage = _t("Error sending email: {0}", _t("No identity matches the sender/from field: {0}", e.getSender()));
                log.error("Error sending email", e);
            }
            catch (AddressException e) {
                statusMessage = _t("Error sending email: {0}", _t("Address doesn't contain an Email Destination or an external address: {0}", e.getRef()));
                log.error("Error sending email", e);
            }
            catch (Exception e) {
                statusMessage = _t("Error sending email: {0}", e.getLocalizedMessage());
                log.error("Error sending email", e);
            }

        try {
            out.println(statusMessage);
        } catch (IOException e) {
            log.error("Can't write output to HTML page", e);
        }
        return EVAL_PAGE;
    }

    /**
     * 
     * @param sender Can be a (Base64-encoded) email identity key or a name plus
     * an email identity key.
     */
    public void setSender(String sender) {
        this.senderAddress = sender;
    }

    public String getSender() {
        return senderAddress;
    }

    void addRecipient(RecipientType type, Address address) {
        recipients.add(new Recipient(type, address));
    }

    void addAttachment(String origFilename, String tempFilename) {
        attachments.add(new FileAttachment(origFilename, tempFilename));
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSubject() {
        return subject;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setIncludeSentTime(boolean includeSentTime) {
        this.includeSentTime = includeSentTime;
    }
 
    public boolean getIncludeSentTime() {
        return includeSentTime;
    }
 
    private static class Recipient {
        RecipientType type;
        Address address;
        
        public Recipient(RecipientType type, Address address) {
            this.type = type;
            this.address = address;
        }
    }
}
