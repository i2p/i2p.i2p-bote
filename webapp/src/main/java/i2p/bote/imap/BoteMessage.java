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

package i2p.bote.imap;

import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.Property;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Header;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;

import i2p.bote.Util;
import i2p.bote.email.Email;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.util.GeneralHelper;

/**
 * A wrapper around {@link Email} that implements the
 * {@link org.apache.james.mailbox.store.mail.model.MailboxMessage} interface.
 */
public class BoteMessage implements MailboxMessage {
    private static final byte[] CRLF = new byte[] {13, 10};
    
    private Email email;
    private MailboxId folderName;
    private long modSeq;
    private MessageUid uid;

    BoteMessage(Email email, MailboxId folderName) {
        this.email = email;
        this.folderName = folderName;
    }
    
    /**
     * Copy constructor
     * @param original
     * @throws MessagingException
     * @throws IOException
     */
    BoteMessage(BoteMessage original) throws MessagingException, IOException {
        email = new Email(original.email);
        folderName = original.folderName;
    }
    
    Email getEmail() {
        return email;
    }

    @Override
    public MessageId getMessageId() {
        return new BoteMessageId(email.getMessageID());
    }
    
    @Override
    public int compareTo(MailboxMessage anotherMsg) {
        return uid.compareTo(anotherMsg.getUid());
    }

    @Override
    public Flags createFlags() {
        try {
            return email.getFlags();
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getBodyContent() throws IOException {
        try {
            return email.getRawInputStream();
        } catch (MessagingException e) {
            throw new IOException(e);
        }
    }

    @Override
    public long getBodyOctets() {
        try {
            return Util.readBytes(getBodyContent()).length;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getFullContent() throws IOException {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            
            // The natural way to do this would be to call email.writeTo(stream) and be done with it.
            // That works unless there is a non-text attachment, which will not be decoded correctly.
            // It looks like James sends the attachment unharmed in its original encoding
            // (i.e. Content-Transfer-Encoding: binary), but Thunderbird treats it as 8bit and
            // changes zero bytes to spaces (0x20). CR/LFs are altered, too.
            // The ugly workaround is to write out the email one part at a time (if type=multipart),
            // reencoding binary attachments to base64.
            stream.write(Util.readBytes(getHeaderContent()));
            stream.write(CRLF);
            
            if (email.isMimeType("multipart/*")) {
                String contentType = email.getContentType();
                String boundary = contentType.substring(contentType.indexOf("boundary=") + "boundary=".length());
                if (boundary.startsWith("\""))
                    boundary = boundary.substring(1);
                if (boundary.endsWith("\""))
                    boundary = boundary.substring(0, boundary.length()-1);
                List<Part> parts = email.getParts();
                for (int partIndex=0; partIndex<parts.size(); partIndex++) {
                    Part part = parts.get(partIndex);
                    stream.write("--".getBytes());
                    stream.write(boundary.getBytes());
                    stream.write(CRLF);
                    if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                        // write headers
                        @SuppressWarnings("unchecked")
                        Enumeration<Header> headers = part.getAllHeaders();
                        while (headers.hasMoreElements()) {
                            Header header = headers.nextElement();
                            if ("Content-Transfer-Encoding".equals(header.getName()))
                                stream.write("Content-Transfer-Encoding: base64".getBytes());
                            else {
                                stream.write(header.getName().getBytes());
                                stream.write(": ".getBytes());
                                stream.write(header.getValue().getBytes());
                            }
                            stream.write(CRLF);
                        }
                        stream.write(CRLF);
                        
                        // write content
                        byte[] contentBytes = Util.readBytes(part.getInputStream());
                        String base64Str = new String(com.lambdaworks.codec.Base64.encode(contentBytes));
                        while (base64Str.length() > 78) {
                            stream.write(base64Str.substring(0, 78).getBytes());
                            stream.write(CRLF);
                            base64Str = base64Str.substring(78);
                        }
                        if (base64Str.length() > 0) {
                            stream.write(base64Str.getBytes());
                            stream.write(CRLF);
                        }
                        stream.write(CRLF);
                    }
                    else
                        part.writeTo(stream);
                    stream.write(CRLF);
                }
                stream.write("--".getBytes());
                stream.write(boundary.getBytes());
                stream.write("--".getBytes());
            } else
                // not a multipart email, so write the content unaltered
                stream.write(Util.readBytes(email.getRawInputStream()));
            stream.write(CRLF);
            return new ByteArrayInputStream(stream.toByteArray());
        } catch (MessagingException e) {
            throw new IOException(e);
        }
    }

    @Override
    public long getFullContentOctets() {
        try {
            return Util.readBytes(getFullContent()).length;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getHeaderContent() throws IOException {
        Address[] from;
        Address sender;
        Address[] to;
        Address[] cc;
        Address[] bcc;
        Address[] replyTo;
        try {
            // Backup current Addresses
            from = email.getFrom();
            sender = email.getSender();
            to = email.getToAddresses();
            cc = email.getCCAddresses();
            bcc = email.getBCCAddresses();
            replyTo = email.getReplyToAddresses();

            // Insert names from addressbook if present
            Address[] fromAddrs = email.getFrom();
            if (fromAddrs != null) {
                insertNames(fromAddrs);
                email.setFrom((Address) null);
            }
            email.addFrom(fromAddrs);
            Address senderAddr = email.getSender();
            if (senderAddr != null)
                email.setSender(insertName(senderAddr));
            insertNames(RecipientType.TO);
            insertNames(RecipientType.CC);
            insertNames(RecipientType.BCC);
            Address[] replyToAddrs = email.getReplyToAddresses();
            if (replyToAddrs != null) {
                insertNames(replyToAddrs);
                email.setReplyTo(replyToAddrs);
            }

            @SuppressWarnings("unchecked")
            List<String> headerLines = Collections.list(email.getAllHeaderLines());
            StringBuilder oneString = new StringBuilder();
            for (String headerLine: headerLines) {
                oneString.append(headerLine);
                oneString.append("\r\n");   // RFC 822 says to use CRLF for newlines
            }

            // Revert recipients to previous state to maintain signature validity
            email.setFrom((Address) null);
            email.addFrom(from);
            email.setSender(sender);
            email.setRecipients(RecipientType.TO, to);
            email.setRecipients(RecipientType.CC, cc);
            email.setRecipients(RecipientType.BCC, bcc);
            email.setReplyTo(replyTo);

            byte[] bytes = oneString.toString().getBytes("UTF-8");   // should only contain ASCII which is compatible
            return new ByteArrayInputStream(bytes);
        } catch (MessagingException e) {
            throw new IOException(e);
        }
    }

    private void insertNames(RecipientType type) throws MessagingException, IOException {
        Address[] recipients = email.getRecipients(type);
        if (recipients != null) {
            insertNames(recipients);
            email.setRecipients(type, recipients);
        }
    }

    private void insertNames(Address[] addresses) throws MessagingException, IOException {
        for (int i = 0; i < addresses.length; i++) {
            addresses[i] = insertName(addresses[i]);
        }
    }

    private Address insertName(Address address) throws MessagingException, IOException {
        try {
            String nameAndDest = GeneralHelper.getImapNameAndDestination(address.toString());
            return new InternetAddress(nameAndDest);
        } catch (PasswordException e) {
        } catch (GeneralSecurityException e) {
        }
        return null;
    }

    @Override
    public Date getInternalDate() {
        return email.getCreateTime();
    }

    @Override
    public MailboxId getMailboxId() {
        return folderName;
    }

    @Override
    public String getMediaType() {
        try {
            return email.getContentType();
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getModSeq() {
        return modSeq;
    }

    @Override
    public List<Property> getProperties() {
        return Collections.emptyList();
    }

    @Override
    public List<MessageAttachment> getAttachments() {
        // TODO: Implement
        return Collections.emptyList();
    }

    @Override
    public String getSubType() {
        return "";
    }

    @Override
    public Long getTextualLineCount() {
        try {
            return Long.valueOf(email.getLineCount());
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public void setUid(MessageUid uid) {
        this.uid = uid;
    }

    public MessageUid getUid() {
        return uid;
    }

    @Override
    public boolean isAnswered() {
        return email.isReplied();
    }

    @Override
    public boolean isDeleted() {
        return email.isDeleted();
    }

    @Override
    public boolean isDraft() {
        return false;   // not supported
    }

    @Override
    public boolean isFlagged() {
        return false;   // not supported
    }

    @Override
    public boolean isRecent() {
        try {
            return email.getFlags().contains(Flag.RECENT);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isSeen() {
        return !email.isUnread();
    }

    @Override
    public void setFlags(Flags flags) {
        try {
            email.setFlags(flags, true);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setModSeq(long modSeq) {
        this.modSeq = modSeq;
    }
    
    @Override
    public String toString() {
        return email.toString();
    }
}