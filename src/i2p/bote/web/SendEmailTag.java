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

import static i2p.bote.Util._;
import i2p.bote.I2PBote;
import i2p.bote.email.Email;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.activation.MimetypesFileTypeMap;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import net.i2p.util.Log;

public class SendEmailTag extends BodyTagSupport {
    private static final long serialVersionUID = 5746062176954959787L;
    
    private Log log = new Log(SendEmailTag.class);
    private String senderAddress;
    private List<Recipient> recipients = new ArrayList<Recipient>();
    private List<Attachment> attachments = new ArrayList<Attachment>();
    private String subject;
    private String message;
    
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
    public int doEndTag() {
        JspWriter out = pageContext.getOut();

        Email email = new Email();
        String statusMessage;
        if (recipients.isEmpty())
            statusMessage = _("Error: Please add at least one recipient.");
        else
            try {
                // set addresses
                email.setSender(new InternetAddress(senderAddress));
                email.setSubject(subject, "UTF-8");
                for (Recipient recipient: recipients)
                    email.addRecipient(recipient.type, recipient.address);
                email.fixAddresses();
                
                // set the text and add attachments
                if (attachments.isEmpty())
                    email.setText(message, "UTF-8");
                else {
                    Multipart multiPart = new MimeMultipart();
                    MimeBodyPart textPart = new MimeBodyPart();
                    textPart.setText(message, "UTF-8");
                    multiPart.addBodyPart(textPart);
                    
                    attach(multiPart, attachments);
                    email.setContent(multiPart);
                }
    
                // send the email
                I2PBote.getInstance().sendEmail(email);
                
                // delete attachment temp files
                for (Attachment attachment: attachments) {
                    File tempFile = new File(attachment.tempFilename);
                    if (!tempFile.delete())
                        log.error("Can't delete file: <" + tempFile.getAbsolutePath() + ">");
                }
                
                statusMessage = _("The email has been queued for sending.");
            }
            catch (Exception e) {
                statusMessage = _("Error sending email: {0}", e.getLocalizedMessage());
                log.error("Error sending email", e);
            }

        try {
            out.println(statusMessage);
        } catch (IOException e) {
            log.error("Can't write output to HTML page", e);
        }
        return EVAL_PAGE;
    }

    private void attach(Multipart multiPart, List<Attachment> attachments) throws MessagingException {
        for (Attachment attachment: attachments) {
            final String mimeType = getMimeType(attachment);
            
            MimeBodyPart attachmentPart = new MimeBodyPart();
            FileDataSource dataSource = new FileDataSource(attachment.tempFilename) {
                @Override
                public String getContentType() {
                    return mimeType;
                }
            };
            attachmentPart.setDataHandler(new DataHandler(dataSource));
            attachmentPart.setFileName(attachment.origFilename);
            multiPart.addBodyPart(attachmentPart);
        }
    }
    
    /**
     * Returns the MIME type for an <code>Attachment</code>. MIME detection is done with
     * JRE classes, so only a small number of MIME types are supported.<p/>
     * It might be worthwhile to use the mime-util library which a much better job:
     * {@link http://sourceforge.net/projects/mime-util/files/}.
     * @param attachment
     * @return
     */
    private String getMimeType(Attachment attachment) {
        MimetypesFileTypeMap mimeTypeMap = new MimetypesFileTypeMap();
        String mimeType = mimeTypeMap.getContentType(attachment.origFilename);
        if (mimeType != null)
            return mimeType;
        
        InputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(attachment.tempFilename));
            mimeType = URLConnection.guessContentTypeFromStream(inputStream);
            if (mimeType != null)
                return mimeType;
        } catch (IOException e) {
            log.error("Can't read file: <" + attachment.tempFilename + ">", e);
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                log.error("Can't close file: <" + attachment.tempFilename + ">", e);
            }
        }
        
        return "application/octet-stream";
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
        attachments.add(new Attachment(origFilename, tempFilename));
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

    private static class Recipient {
        RecipientType type;
        Address address;
        
        public Recipient(RecipientType type, Address address) {
            this.type = type;
            this.address = address;
        }
    }

    private static class Attachment {
        String origFilename;
        String tempFilename;
        
        public Attachment(String origFilename, String tempFilename) {
            this.origFilename = origFilename;
            this.tempFilename = tempFilename;
        }
    }
}