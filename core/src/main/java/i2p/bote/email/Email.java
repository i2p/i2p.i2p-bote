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

package i2p.bote.email;

import com.nettgryppa.security.HashCash;

import net.i2p.data.Base64;
import net.i2p.util.Log;
import net.i2p.util.SystemVersion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MailDateFormat;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import SevenZip.Compression.LZMA.Decoder;
import SevenZip.Compression.LZMA.Encoder;
import i2p.bote.UniqueId;
import i2p.bote.crypto.CryptoFactory;
import i2p.bote.crypto.CryptoImplementation;
import i2p.bote.crypto.KeyUpdateHandler;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.fileencryption.PasswordHolder;
import i2p.bote.packet.dht.UnencryptedEmailPacket;

public class Email extends MimeMessage {
    private static final String SIGNATURE_HEADER = "X-I2PBote-Signature";   // contains the sender's base64-encoded signature
    private static final String SIGNATURE_VALID_HEADER = "X-I2PBote-Sig-Valid";   // contains the string "true" or "false"
    private static final String[] HEADER_WHITELIST = new String[] {
        "From", "Sender", "To", "CC", "BCC", "Reply-To", "Subject", "Date", "MIME-Version", "Content-Type",
        "Content-Transfer-Encoding", "In-Reply-To", "X-HashCash", "X-Priority", SIGNATURE_HEADER
    };
    private static final int MAX_HEADER_LENGTH = 998;   // Maximum length of a header line, see RFC 5322
    private enum CompressionAlgorithm {UNCOMPRESSED, LZMA};   // The first byte in a compressed email
    
    private Log log = new Log(Email.class);
    private UniqueId messageId;
    private EmailMetadata metadata;
    private boolean includeSendTime;

    /**
     * @param includeSendTime Whether to add a "Date" header.
     */
    public Email(boolean includeSendTime) {
        super(Session.getDefaultInstance(new Properties()));
        messageId = new UniqueId();
        metadata = new EmailMetadata();
        this.includeSendTime = includeSendTime;
    }

    /**
     * Creates an <code>Email</code> from a file containing an <strong>uncompressed</strong> MIME email
     * and another file containing metadata. If the metadata file doesn't exist, the metadata will be
     * empty.
     * @param emailStream
     * @param metadataStream
     * @param passwordHolder
     * @throws MessagingException
     * @throws IOException
     * @throws PasswordException 
     * @throws GeneralSecurityException 
     */
    public Email(InputStream emailStream, InputStream metadataStream, PasswordHolder passwordHolder) throws MessagingException, IOException, PasswordException, GeneralSecurityException {
        this(emailStream, false);
        if (metadataStream != null)
            metadata = new EmailMetadata(metadataStream);
        else
            metadata = new EmailMetadata();
    }
    
    /**
     * Creates an <code>Email</code> from an InputStream containing a compressed or uncompressed MIME email.
     * @param inputStream
     * @param compressed <code>true</code> if the stream contains compressed data
     * @throws MessagingException 
     * @throws IOException 
     */
    public Email(InputStream inputStream, boolean compressed) throws MessagingException, IOException {
        super(Session.getDefaultInstance(new Properties()), compressed?Email.decompress(inputStream):inputStream);
        messageId = new UniqueId();
        includeSendTime = getSentDate() != null;
        metadata = new EmailMetadata();
    }

   /**
    * Creates an <code>Email</code> from a byte array containing a <strong>compressed</strong> MIME email.
    * @param bytes
    * @throws MessagingException 
    * @throws IOException 
    */
    public Email(byte[] bytes) throws MessagingException, IOException {
        this(new ByteArrayInputStream(bytes), true);
        messageId = new UniqueId();
        metadata = new EmailMetadata();
    }

    /**
     * Copy constructor 
     * @throws IOException 
     * @throws MessagingException
     */
    public Email(Email original) throws MessagingException, IOException {
        super(original);
        messageId = original.messageId;
        metadata = original.metadata;
        includeSendTime = original.includeSendTime;
    }
    
    /**
     * Sets the message text and adds attachments.
     * @param text
     * @param attachments Can be <code>null</code>
     * @throws MessagingException
     */
    public void setContent(String text, List<Attachment> attachments) throws MessagingException {
        if (attachments==null || attachments.isEmpty())
            setText(text, "UTF-8");
        else {
            Multipart multiPart = new MimeMultipart();
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(text, "UTF-8");
            multiPart.addBodyPart(textPart);
            
            attach(multiPart, attachments);
            setContent(multiPart);
        }
    }
    
    /**
     * Reads attachments from files and adds them to a {@link Multipart}.<br/>
     * Attachments are not encoded in Base64 (because it doesn't compress well),
     * but in 8 bit encoding.
     * @param multiPart
     * @param attachments
     * @throws MessagingException
     * @TODO use 8 bit encoding only before compressing an email, use Base64 when writing an email to a file.
     */
    private void attach(Multipart multiPart, List<Attachment> attachments) throws MessagingException {
        for (Attachment attachment: attachments) {
            MimeBodyPart attachmentPart = new MimeBodyPart() {
                @Override
                public void updateHeaders() throws MessagingException {
                    super.updateHeaders();
                    setHeader("Content-Transfer-Encoding", "binary");
                }
            };
            attachmentPart.setDataHandler(attachment.getDataHandler());
            attachmentPart.setFileName(attachment.getFileName());
            multiPart.addBodyPart(attachmentPart);
        }
    }
    
    public void setMetadata(EmailMetadata metadata) {
        this.metadata = metadata;
    }
    
    /**
     * Returns the metadata for this email. This method never returns <code>null</code>.
     */
    public EmailMetadata getMetadata() {
        return metadata;
    }

    /**
     * Returns <code>true</code> if the sender is anonymous, or
     * <code>false</code> if the email contains a sender.
     * @throws MessagingException
     */
    public boolean isAnonymous() throws MessagingException {
        String sender = getOneFromAddress();
        return sender==null || "Anonymous".equalsIgnoreCase(sender);
    }
    
    /**
     * Returns the first value of the "from:" header (or the value of the
     * "sender:" header if "from:" is not there), or <code>null</code> if there
     * is none.
     */
    public String getOneFromAddress() throws MessagingException {
        Address[] fromAddresses = getFrom();
        if (fromAddresses==null || fromAddresses.length==0)
            return null;
        else
            return fromAddresses[0].toString();
    }
    
    public String getOneRecipient() throws MessagingException {
        Address[] recipients = getAllRecipients();
        if (recipients==null || recipients.length==0)
            return null;
        else
            return recipients[0].toString();
    }
    
    public void setHashCash(HashCash hashCash) throws MessagingException {
        setHeader("X-HashCash", hashCash.toString());
    }

    /**
     * Removes all headers that are not on the whitelist, and initializes some
     * basic header fields.<br/>
     * Called by {@link #saveChanges()}, see JavaMail JavaDoc.
     * @throws MessagingException
     */
    @Override
    public void updateHeaders() throws MessagingException {
        super.updateHeaders();
        scrubHeaders();
        removeRecipientNames();
        
        // Depending on includeSendTime, set the send time or remove the send time field
        if (includeSendTime) {
            // Ensure the "Date" field is set in UTC time, using the English locale.
            MailDateFormat formatter = new MailDateFormat();
            formatter.setTimeZone(TimeZone.getTimeZone("GMT"));   // always use UTC for outgoing mail
            if (getSentDate() == null)
                setHeader("Date", formatter.format(new Date()));
            else
                setHeader("Date", formatter.format(getSentDate()));
        }
        else
            removeHeader("Date");
    }

    /**
     * A global unique number, to ensure uniqueness of generated strings.
     */
    private static AtomicInteger androidUniqueID = new AtomicInteger();
    /**
     * Update the Message-ID header.  This method is called
     * by the <code>updateHeaders</code>. The algorithm for
     * choosing a Message-ID is overridden on Android because
     * the default calls <code>InetAddress.getLocalHost()</code>
     * which triggers a <code>NetworkOnMainThreadException</code>.
     */
    protected void updateMessageID() throws MessagingException {
        if (SystemVersion.isAndroid()) {
            String suffix = "droidjavamailuser@localhost";

            StringBuffer s = new StringBuffer();

            // Unique string is <hashcode>.<id>.<currentTime>.JavaMail.<suffix>
            s.append(s.hashCode()).append('.').
            append(androidUniqueID.getAndIncrement()).append('.').
            append(System.currentTimeMillis()).append('.').
            append("JavaMail.").
            append(suffix);
            setHeader("Message-ID", 
                    "<" + s.toString() + ">");
        } else {
            super.updateMessageID();
        }
          
    }

    /**
     * Creates a digital signature of the email and stores it in the
     * <code>SIGNATURE_HEADER</code> header field. It also removes the
     * <code>SIGNATURE_VALID_HEADER</code> header. If there is a signature
     * already, it is replaced.<br/>
     * The signature is computed over the stream representation of the
     * email, minus the signature header if it is present.<br/>
     * The signature includes the ID number of the {@link CryptoImplementation}
     * used (signature lengths can be different for the same algorithm).
     * @param senderIdentity
     * @param keyUpdateHandler Needed for updating the signature key after signing (see {@link CryptoImplementation#sign(byte[], PrivateKey, KeyUpdateHandler)})
     * @throws MessagingException
     * @throws GeneralSecurityException 
     * @throws PasswordException 
     */
    public void sign(EmailIdentity senderIdentity, KeyUpdateHandler keyUpdateHandler) throws MessagingException, GeneralSecurityException, PasswordException {
        removeHeader(SIGNATURE_HEADER);   // make sure there is no existing signature which would make the new signature invalid
        removeHeader(SIGNATURE_VALID_HEADER);   // remove the signature validity flag before signing
        CryptoImplementation cryptoImpl = senderIdentity.getCryptoImpl();
        PrivateKey privateSigningKey = senderIdentity.getPrivateSigningKey();
        byte[] signature = cryptoImpl.sign(toByteArray(), privateSigningKey, keyUpdateHandler);
        String foldedSignature = foldSignature(cryptoImpl.getId() + "_" + Base64.encode(signature));
        setHeader(SIGNATURE_HEADER, foldedSignature);
    }
    
    /**
     * Breaks up a signature into pieces no longer than <code>MAX_HEADER_LENGTH</code>.
     * Unlike {@link javax.mail.internet.MimeUtility#fold(int, String)}, this method
     * doesn't require the string to contain whitespace.
     * @param signature
     */
    private String foldSignature(String signature) {
        StringBuilder folded = new StringBuilder();
        int used = SIGNATURE_HEADER.length() + 2;   // account for header name and ": " between name and value
        while (used+signature.length() > MAX_HEADER_LENGTH) {
            folded.append(signature.substring(0, MAX_HEADER_LENGTH));
            signature = signature.substring(MAX_HEADER_LENGTH);
            if (!signature.isEmpty())
                folded.append("\r\n ");
            used = 0;
        }
        if (!signature.isEmpty())
            folded.append(signature);
        return folded.toString();
    }
    
    /**
     * The counterpart to {@link #foldSignature(String)}: Reassembles a signature string
     * by removing newlines and spaces.
     */
    private String unfoldSignature(String folded) {
        return folded.replaceAll("\r\n ", "");
    }
    
    /**
     * Verifies the signature and sets the <code>SIGNATURE_VALID_HEADER</code>
     * header field accordingly.
     */
    public void setSignatureFlag() {
        try {
            removeHeader(SIGNATURE_VALID_HEADER);   // remove the signature validity flag before verifying
            boolean valid = verifySignature();
            setHeader(SIGNATURE_VALID_HEADER, String.valueOf(valid));
        } catch (MessagingException e) {
            log.error("Cannot get header field: " + SIGNATURE_VALID_HEADER, e);
        }
    }
    
    public void removeSignatureFlag() {
        try {
            removeHeader(SIGNATURE_VALID_HEADER);
        } catch (MessagingException e) {
            log.error("Cannot remove header field: " + SIGNATURE_VALID_HEADER, e);
        }
    }
    
    /**
     * Verifies that the email contains a valid signature.<br/>
     * If the <code>SIGNATURE_VALID_HEADER</code> is present, its value is
     * used.<br/>
     * If not, the value of the <code>SIGNATURE_HEADER</code> header
     * field is verified (which is more CPU intensive).
     * @return <code>true</code> if the signature is valid; <code>false</code>
     * if it is invalid or an error occurred.
     */
    public boolean isSignatureValid() {
        try {
            String[] sigValidFlag = getHeader(SIGNATURE_VALID_HEADER);
            if (sigValidFlag==null || sigValidFlag.length==0)
                return verifySignature();
            else
                return "true".equalsIgnoreCase(sigValidFlag[0]);
        } catch (MessagingException e) {
            log.error("Cannot get header field: " + SIGNATURE_VALID_HEADER, e);
            return false;
        }
    }

    /**
     * Verifies that the <code>SIGNATURE_HEADER</code> header field
     * contains a valid signature.<br/>
     * The <code>SIGNATURE_VALID_HEADER</code> header field must not be
     * present when this method is called.
     * @return <code>true</code> if the signature is valid; <code>false</code>
     * if it is invalid or missing, or an error occurred.
     */
    private boolean verifySignature() {
        String[] signatureHeaders;
        try {
            signatureHeaders = getHeader(SIGNATURE_HEADER);
        } catch (MessagingException e) {
            log.error("Cannot get header field: " + SIGNATURE_HEADER, e);
            return false;
        }
        if (signatureHeaders==null || signatureHeaders.length<=0)
            return false;
        String signatureHeader = signatureHeaders[0];
        String unfoldedSignatureHeader = unfoldSignature(signatureHeader);
        
        // the crypto implementation ID is the number before the underscore
        int _index = unfoldedSignatureHeader.indexOf('_');
        if (_index < 0)
            return false;
        String cryptoImplIdString = unfoldedSignatureHeader.substring(0, _index);
        int cryptoImplId = 0;
        try {
            cryptoImplId = Integer.valueOf(cryptoImplIdString);
        }
        catch (NumberFormatException e) {
            return false;
        }
        CryptoImplementation cryptoImpl = CryptoFactory.getInstance(cryptoImplId);
        
        // the actual signature is everything after the underscore
        String base64Signature = unfoldedSignatureHeader.substring(_index + 1);
        try {
            removeHeader(SIGNATURE_HEADER);   // remove the signature before verifying
            byte[] signature = Base64.decode(base64Signature);
            EmailDestination senderDestination = new EmailDestination(getOneFromAddress());
            return cryptoImpl.verify(toByteArray(), signature, senderDestination.getPublicSigningKey());
        } catch (Exception e) {
            log.error("Cannot verify email signature. Email: [" + this + "]", e);
            return false;
        } finally {
            try {
                setHeader(SIGNATURE_HEADER, signatureHeader);
            } catch (MessagingException e) {
                log.error("Cannot set signature header field.", e);
                return false;
            }
        }
    }
    
    /**
     * If all addresses are valid, nothing happens.
     * @throws AddressException if one or more address fields contain an invalid address.
     * @throws MessagingException on underlying failures.
     */
    public void checkAddresses() throws MessagingException {
        // Check sender
        Collection<Address> fromAddresses = getAllFromAddresses();
        for (Address address : fromAddresses) {
            checkSender(address);
        }

        // Check all other addresses
        Collection<Address> addresses = getAllAddresses(false);
        for (Address address: addresses) {
            checkRecipient(address);
        }
    }
    
    public static void checkSender(Address address) throws AddressException {
        // same rules as for recipients, except senders can be anonymous
        if (!"Anonymous".equalsIgnoreCase(address.toString()))
            checkRecipient(address);
    }
    
    public static void checkRecipient(Address address) throws AddressException {
        String addr = address.toString();
        try {
            new EmailDestination(addr);
        }
        catch (GeneralSecurityException e) {
            // check for external address
            // InternetAddress accepts addresses without a domain, so check that there is a '.' after the '@'
            if (addr.indexOf('@') >= addr.indexOf('.'))
                throw new AddressException("Address doesn't contain an Email Destination or an external address", addr);
        }
    }
    /*
    public void fixAddresses() throws MessagingException {
        List<Header> addressHeaders = getAllAddressHeaders();
        for (String headerName: ADDRESS_HEADERS)
            removeHeader(headerName);
        for (Header header: addressHeaders) {
            String fixedAddress = Util.fixAddress(header.getValue());
            addHeader(header.getName(), fixedAddress);
        }
    }*/
    
    public Collection<Address> getAllFromAddresses() throws MessagingException {
        Collection<Address> addresses = new ArrayList<Address>();

        Address[] from = getFrom();
        if (from != null)
            addresses.addAll(Arrays.asList(from));

        Address sender = getSender();
        if (sender != null)
            addresses.addAll(Arrays.asList(sender));

        return addresses;
    }
    
    public Collection<Address> getAllAddresses(boolean includeFrom) throws MessagingException {
        Collection<Address> addresses = new ArrayList<Address>();

        // If we want to check validity, fetch these separately
        // (because these can contain 'anonymous').
        if (includeFrom) {
            addresses.addAll(getAllFromAddresses());
        }

        Address[] recipients = getAllRecipients();
        if (recipients != null)
            addresses.addAll(Arrays.asList(recipients));

        // Reply-To should not be anonymous, check with recipients
        Address[] replyTo = getReplyToAddresses();
        if (replyTo != null)
            addresses.addAll(Arrays.asList(replyTo));

        return addresses;
    }
    
    /**
     * Returns the values of all "Reply-To" headers (usually zero or one).
     * Unlike {@link #getReplyTo()}, this method does not return
     * the "From" address if there is no "Reply To" address.<br/>
     * Not to be confused with {@link #getReplyAddress(Identities)}.
     * @throws MessagingException
     */
    public Address[] getReplyToAddresses() throws MessagingException {
        String s = getHeader("Reply-To", ",");
        return (s == null) ? null : InternetAddress.parseHeader(s, true);
    }
    
    /**
     * Returns the address replies to this email should be sent to.
     * If a <code>Reply-To</code> header exists, its value is returned.
     * Otherwise, if the recipient is a local identity (i.e. the email
     * was sent by somebody else), the sender is used; if the recipient
     * is not a local identity (sender was us), the recipient is used.
     * <br/>
     * Not to be confused with {@link #getReplyToAddresses()}.
     * @param identities
     * @throws MessagingException
     * @throws GeneralSecurityException 
     * @throws IOException 
     * @throws PasswordException 
     */
    public String getReplyAddress(Identities identities) throws MessagingException, PasswordException, IOException, GeneralSecurityException {
        Address[] replyTo = getReplyToAddresses();
        if (replyTo!=null && replyTo.length>0)
            return replyTo[0].toString();
        else {
            String sender = getOneFromAddress();
            EmailIdentity senderIdentity = identities.extractIdentity(sender);
            if (senderIdentity != null)
                return getOneRecipient();   // sent by local user, so reply to recipient
            else
                return sender;   // sent by other party, so reply to sender
        }
    }
    
    public Address[] getToAddresses() throws MessagingException {
        return getRecipients(RecipientType.TO);
    }
    
    public Address[] getCCAddresses() throws MessagingException {
        return getRecipients(RecipientType.CC);
    }
    
    public Address[] getBCCAddresses() throws MessagingException {
        return getRecipients(RecipientType.BCC);
    }
    
    private byte[] toByteArray() throws MessagingException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try {
            writeTo(byteStream);
        } catch (IOException e) {
            throw new MessagingException("Cannot write email to ByteArrayOutputStream.", e);
        }
        return byteStream.toByteArray();
    }
    
    /**
     * Removes all mail headers except the ones in <code>HEADER_WHITELIST</code>.
     * @throws MessagingException 
     */
    private void scrubHeaders() throws MessagingException {
        @SuppressWarnings("unchecked")
        List<Header> nonMatchingHeaders = Collections.list(getNonMatchingHeaders(HEADER_WHITELIST));
        for (Header header: nonMatchingHeaders)
            if (header != null) {
                log.debug("Removing all instances of non-whitelisted header <" + header.getName() + ">");
                removeHeader(header.getName());
            }
    }
    
    /**
     * Removes everything but the email destination from all recipient fields,
     * in order to keep local contact names private.
     * @throws MessagingException
     */
    private void removeRecipientNames() throws MessagingException {
        removeRecipientNames(RecipientType.TO);
        removeRecipientNames(RecipientType.CC);
        removeRecipientNames(RecipientType.BCC);
    }
    
    private void removeRecipientNames(Message.RecipientType type) throws MessagingException {
        Address[] recipients = getRecipients(type);
        if (recipients != null) {
            removeRecipientNames(recipients);
            setRecipients(type, recipients);
        }
    }

    private void removeRecipientNames(Address[] recipients) {
        for (int i = 0; i < recipients.length; i++) {
            removeRecipientName((InternetAddress) recipients[i]);
        }
    }

    private void removeRecipientName(InternetAddress address) {
        String addr = address.getAddress();
        String dest = EmailDestination.extractBase64Dest(addr);
        if (dest != null)
            try {
                address.setPersonal(null);
            } catch (UnsupportedEncodingException e) {}
        // If there is no email destination, assume it is an external address and don't change it
    }

    public void removeBoteSuffixes() throws MessagingException {
        Address[] from = getFrom();
        if (from != null) {
            removeBoteSuffixes(from);
            setFrom(from[0]);
            if (from.length > 1)
                addFrom(Arrays.copyOfRange(from, 1, from.length));
        }
        Address sender = getSender();
        if (sender != null) {
            removeBoteSuffix((InternetAddress) sender);
            setSender(sender);
        }

        Address[] to = getRecipients(RecipientType.TO);
        if (to != null) {
            removeBoteSuffixes(to);
            setRecipients(RecipientType.TO, to);
        }
        Address[] cc = getRecipients(RecipientType.CC);
        if (cc != null) {
            removeBoteSuffixes(cc);
            setRecipients(RecipientType.CC, cc);
        }
        Address[] bcc = getRecipients(RecipientType.BCC);
        if (bcc != null) {
            removeBoteSuffixes(bcc);
            setRecipients(RecipientType.BCC, bcc);
        }
        Address[] replyTo = getReplyToAddresses();
        if (replyTo != null) {
            removeBoteSuffixes(replyTo);
            setReplyTo(replyTo);
        }
    }

    private void removeBoteSuffixes(Address[] addresses) {
        for (int i = 0; i < addresses.length; i++) {
            removeBoteSuffix((InternetAddress) addresses[i]);
        }
    }

    /**
     * Removes the "@bote" suffix which an email destination may have
     * added at the end so the email client accepts it.
     */
    private void removeBoteSuffix(InternetAddress address) {
        address.setAddress(removeBoteSuffix(address.getAddress()));
    }

    public static String removeBoteSuffix(String addr) {
        if (addr.endsWith("@bote"))
            return addr.substring(0, addr.indexOf("@bote"));
        else
            return addr;
    }
    
    /**
     * 
     * @param messageIdString Must be a 44-character Base64-encoded string.
     */
    public void setMessageID(String messageIdString) {
        this.messageId = new UniqueId(messageIdString);
    }
    
    public void setMessageID(UniqueId messageId) {
        this.messageId = messageId;
    }
    
    @Override
    public String getMessageID() {
        return messageId.toBase64();
    }

    /** @see EmailMetadata#setRecent(recent) */
    public void setRecent(boolean recent) {
        metadata.setRecent(recent);
    }

    /** @see EmailMetadata#isRecent() */
    public boolean isRecent() {
        return metadata.isRecent();
    }

    /** @see EmailMetadata#setUnread(boolean) */
    public void setUnread(boolean unread) {
        metadata.setUnread(unread);
    }

    /** @see EmailMetadata#isUnread() */
    public boolean isUnread() {
        return metadata.isUnread();
    }

    /** @see EmailMetadata#setCreateTime(Date) */
    public void setCreateTime(Date createTime) {
        metadata.setCreateTime(createTime);
    }
    
    /** @see EmailMetadata#isDelivered() */
    public boolean isDelivered() {
        return metadata.isDelivered();
    }

    /** @see EmailMetadata#setReplied(boolean) */
    public void setReplied(boolean replied) {
        metadata.setReplied(replied);
    }

    /** @see EmailMetadata#isReplied() */
    public boolean isReplied() {
        return metadata.isReplied();
    }

    /** @see EmailMetadata#setDeleted(boolean) */
    public void setDeleted(boolean deleted) {
        metadata.setDeleted(deleted);
    }

    /** @see EmailMetadata#isDeleted() */
    public boolean isDeleted() {
        return metadata.isDeleted();
    }

    /** @see EmailMetadata#getDeliveryPercentage() */
    public int getDeliveryPercentage() {
        return metadata.getDeliveryPercentage();
    }
    
    /** @see EmailMetadata#getCreateTime() */
    public Date getCreateTime() {
        return metadata.getCreateTime();
    }
    
    @Override
    public Date getReceivedDate() {
        return metadata.getReceivedDate();
    }
    
    /**
     * Sets flags for this message. Flags not explicitly stored in metadata
     * will be kept in-memory.
     * @param flag The current state of flags for this message.
     */
    @Override
    public synchronized void setFlags(Flags flag, boolean set) throws MessagingException {
        if (flag.contains(Flag.RECENT))
            setRecent(set);
        if (flag.contains(Flag.SEEN))
            setUnread(!set);
        if (flag.contains(Flag.ANSWERED))
            setReplied(set);
        if (flag.contains(Flag.DELETED))
            setDeleted(set);
        super.setFlags(flag, set);
    }

    /**
     * Updates in-memory flags from stored metadata and returns them.
     */
    @Override
    public Flags getFlags() throws MessagingException {
        super.setFlag(Flag.RECENT, isRecent());
        super.setFlag(Flag.SEEN, !isUnread());
        super.setFlag(Flag.ANSWERED, isReplied());
        super.setFlag(Flag.DELETED, isDeleted());
        return super.getFlags();
    }
    
    /**
     * Updates headers, signs the email, and converts it into one or more email packets.
     * If an error occurs, an empty <code>Collection</code> is returned.
     *
     * @param senderIdentity The sender's Email Identity, or <code>null</code> for anonymous emails
     * @param keyUpdateHandler Needed for updating the signature key after signing (see {@link CryptoImplementation#sign(byte[], PrivateKey, KeyUpdateHandler)})
     * @param bccToKeep All BCC fields in the header section of the email are removed, except this field. If this parameter is <code>null</code>, all BCC fields are written.
     * @param maxPacketSize The size limit in bytes
     * @throws MessagingException
     * @throws GeneralSecurityException If the email cannot be signed
     * @throws PasswordException If the private signing key cannot be updated
     */
    public Collection<UnencryptedEmailPacket> createEmailPackets(EmailIdentity senderIdentity, KeyUpdateHandler keyUpdateHandler, String bccToKeep, int maxPacketSize) throws MessagingException, GeneralSecurityException, PasswordException {
        ArrayList<UnencryptedEmailPacket> packets = new ArrayList<UnencryptedEmailPacket>();
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String[] bccHeaders = null;
        try {
            bccHeaders = getHeader("BCC");
            saveChanges();
            if (bccToKeep!=null && isBCC(bccToKeep))
                setHeader("BCC", bccToKeep);   // set bccToKeep and remove any other existing BCC addresses
            else
                removeHeader("BCC");
            if (!isAnonymous())
                sign(senderIdentity, keyUpdateHandler);
            compressTo(outputStream);
        } catch (IOException e) {
            throw new MessagingException("Can't write the email to an OutputStream.", e);
        } catch (GeneralSecurityException e) {
            throw new GeneralSecurityException("Can't sign email.", e);
        } finally {
            // restore the BCC headers
            removeHeader("BCC");
            if (bccHeaders != null)
                for (String bccAddress: bccHeaders)
                    addHeader("BCC", bccAddress);
        }
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        
        int packetIndex = 0;
        try {
            while (true) {
                UnencryptedEmailPacket packet = new UnencryptedEmailPacket(inputStream, messageId, packetIndex, maxPacketSize);
                packets.add(packet);
                packetIndex++;
                if (inputStream.available() <= 0)
                    break;
            }
        }
        catch (IOException e) {
            log.error("Can't read from ByteArrayInputStream.", e);
        }
        
        // #packets has not been set yet, do it now
        int numPackets = packetIndex;
        for (UnencryptedEmailPacket packet: packets)
            packet.setNumFragments(numPackets);
        
        return packets;
    }
    
    /**
     * Like {@link writeTo(OutputStream)}, but compresses the data if it reduces the size.
     * @param input
     * @throws IOException 
     * @throws MessagingException 
     * @see Encoder
     */
    private void compressTo(OutputStream outputStream) throws IOException, MessagingException {
        // Make an uncompressed byte array
        ByteArrayOutputStream uncompressedStream = new ByteArrayOutputStream();
        writeTo(uncompressedStream);
        byte[] uncompressedArray = uncompressedStream.toByteArray();
        
        // Make a compressed byte array
        ByteArrayInputStream inputStream = new ByteArrayInputStream(uncompressedArray);
        ByteArrayOutputStream compressedStream = new ByteArrayOutputStream();
        Encoder lzmaEncoder = new Encoder();
        lzmaEncoder.SetDictionarySize(1<<20);   // dictionary size = 1 MByte
        lzmaEncoder.SetEndMarkerMode(true);   // by using an end marker, the uncompressed size doesn't need to be stored with the compressed data
        lzmaEncoder.WriteCoderProperties(compressedStream);
        lzmaEncoder.Code(inputStream, compressedStream, -1, -1, null);
        byte[] compressedArray = compressedStream.toByteArray();
        
        // Write the compressed or uncompressed array, whichever is shorter
        if (uncompressedArray.length <= compressedArray.length) {
            outputStream.write(CompressionAlgorithm.UNCOMPRESSED.ordinal());
            outputStream.write(uncompressedArray);
        }
        else {
            outputStream.write(CompressionAlgorithm.LZMA.ordinal());
            outputStream.write(compressedArray);
        }
    }
    
    /**
     * Decompresses the data from an <code>InputStream</code> and returns
     * it as a new <code>InputStream</code>.<br/>
     * (Kind of a poor man's <code>FilterInputStream</code>)
     * @param inputStream
     * @throws IOException
     */
    private static InputStream decompress(InputStream inputStream) throws IOException {
        Decoder lzmaDecoder = new Decoder();
        byte[] lzmaProperties = new byte[Encoder.kPropSize];
        
        int compressionAlgOrdinal = inputStream.read();
        CompressionAlgorithm compressionAlgorithm = null;
        if (compressionAlgOrdinal>=0 && compressionAlgOrdinal<CompressionAlgorithm.values().length)
            compressionAlgorithm = CompressionAlgorithm.values()[compressionAlgOrdinal];
        
        switch(compressionAlgorithm) {
        case UNCOMPRESSED:
            return inputStream;
        case LZMA:
            int bytesRead = inputStream.read(lzmaProperties);
            if (bytesRead < Encoder.kPropSize)
                throw new IOException("Input is too short! Must be at least " + Encoder.kPropSize + " bytes.");
            if (!lzmaDecoder.SetDecoderProperties(lzmaProperties))
                throw new IOException("Incorrect stream properties.");
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            if (!lzmaDecoder.Code(inputStream, outputStream, -1))   // size = -1 means use the end marker
                throw new IOException("Error in data stream");
            return new ByteArrayInputStream(outputStream.toByteArray());
        default:
            throw new IOException("Unknown compression algorithm: " + compressionAlgOrdinal);
        }
    }
    
    /**
     * Tests if <code>address</code> is a BCC address.
     * @param address
     * @throws MessagingException
     */
    private boolean isBCC(String address) throws MessagingException {
        Address[] bccAddresses = getBCCAddresses();
        if (bccAddresses == null)
            return false;
        
        for (Address bccAddress: bccAddresses)
            if (bccAddress.toString().equals(address))
                return true;
        
        return false;
    }
    
    /**
     * Returns the text part of the email. If the email contains no text {@link Part},
     * <code>null</code> is returned. If an error occurs, an error message is returned.
     */
    public String getText() {
        try {
            Object content = getMainTextPart().getContent();
            if (content != null)
                return content.toString();
            else
                return null;
        } catch (Exception e) {
            String errorMsg = "Error reading email content.";
            log.error(errorMsg, e);
            return errorMsg;
        }
    }
    
    /**
     * Returns the <code>Part</code> whose <code>content</code>
     * should be displayed inline.
     * @throws MessagingException 
     * @throws IOException 
     */
    private Part getMainTextPart() throws MessagingException, IOException {
        List<Part> parts = getParts();

        Part mostPreferable = this;
        for (Part part: parts) {
            String disposition = part.getDisposition();
            if (!Part.ATTACHMENT.equalsIgnoreCase(disposition)) {
                // prefer plain text
                if (part.isMimeType("text/plain"))
                    return part;
                else if (part.isMimeType("text/html"))
                    mostPreferable = part;
            }
        }
        return mostPreferable;
    }

    public boolean isContainingAttachments() throws MessagingException, IOException {
        for (Part part : getParts()) {
            if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()))
                return true;
        }
        return false;
    }

    /**
     * Returns the <code>Part</code>s of the email as a <code>List</code>.
     * <code>Part</code>s that are only containers are not included.<br/>
     * The <code>List</code> is sorted in ascending order of depth.<br/>
     * If this method is invoked more than once, the ordering of the elements
     * is the same.
     * @throws IOException 
     * @throws MessagingException 
     */
    public List<Part> getParts() throws MessagingException, IOException {
        return getAllSubparts(this);
    }
    
    /**
     * Returns a <code>List</code> that contains a <code>Part</code>
     * for each descendent of a given <code>Part</code>.
     * @param part
     * @throws MessagingException
     * @throws IOException
     * @see Part
     */
    private List<Part> getAllSubparts(Part part) throws MessagingException, IOException {
        List<Part> parts = new ArrayList<Part>();
        addSubhierarchy(parts, part, 0);
        return parts;
    } 

    // TODO limit recursion depth
    private void addSubhierarchy(List<Part> parts, Part part, int depth) throws MessagingException, IOException {
        if (part.isMimeType("message/rfc822")) {   // nested message
            Part subpart = (Part)part.getContent();
            addSubhierarchy(parts, subpart, depth);
        }
        else if (part.isMimeType("multipart/*")) {
            Multipart subparts = (Multipart)part.getContent();
            for (int i=0; i<subparts.getCount(); i++) {
                Part subpart = subparts.getBodyPart(i);
                addSubhierarchy(parts, subpart, depth);
            }
        }
        else
            parts.add(part);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("MsgId: ").append(getMessageID());
        try {
            result = result.append("From: ").append(getOneFromAddress());
            result = result.append("Recipients: ");
            for (Address recipient: getAllRecipients()) {
                if (result.length() > 1000) {
                    result = result.append("...");
                    break;
                }
                if (result.length() > 0)
                    result = result.append(", ");
                String recipientAddress = recipient.toString();
                if (recipientAddress.length() > 20)
                    result = result.append(recipientAddress).append("...");
                else
                    result = result.append(recipientAddress);
            }
        } catch (MessagingException e) {
            log.error("Error getting sender or recipients.");
            result.append("#Error#");
        }
        return result.toString();
    }
}
