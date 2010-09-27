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

import static i2p.bote.Util._;
import i2p.bote.UniqueId;
import i2p.bote.Util;
import i2p.bote.crypto.CryptoFactory;
import i2p.bote.crypto.CryptoImplementation;
import i2p.bote.packet.UnencryptedEmailPacket;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.activation.MimetypesFileTypeMap;
import javax.mail.Address;
import javax.mail.Header;
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

import net.i2p.data.Base64;
import net.i2p.data.DataFormatException;
import net.i2p.util.Log;
import SevenZip.Compression.LZMA.Decoder;
import SevenZip.Compression.LZMA.Encoder;

import com.nettgryppa.security.HashCash;

public class Email extends MimeMessage {
    private static final String SIGNATURE_HEADER = "X-I2PBote-Signature";   // contains the sender's base64-encoded signature
    private static final String SIGNATURE_VALID_HEADER = "X-I2PBote-Sig-Valid";   // contains the string "true" or "false"
    private static final String[] HEADER_WHITELIST = new String[] {
        "From", "Sender", "To", "CC", "BCC", "Reply-To", "Subject", "Date", "MIME-Version", "Content-Type",
        "Content-Transfer-Encoding", "In-Reply-To", "X-HashCash", "X-Priority", SIGNATURE_HEADER
    };
    private static final String[] ADDRESS_HEADERS = new String[] {"From", "Sender", "To", "CC", "BCC", "Reply-To"};
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
     * @param emailFile
     * @param metadataFile
     * @throws MessagingException
     * @throws IOException
     */
    public Email(File emailFile, File metadataFile) throws MessagingException, IOException {
        this(new FileInputStream(emailFile), false);
        if (metadataFile.exists())
            metadata = new EmailMetadata(metadataFile);
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
    private Email(InputStream inputStream, boolean compressed) throws MessagingException, IOException {
        super(Session.getDefaultInstance(new Properties()), compressed?Email.decompress(inputStream):inputStream);
        messageId = new UniqueId();
        includeSendTime = getSentDate() != null;
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
            final String mimeType = getMimeType(attachment);
            
            MimeBodyPart attachmentPart = new MimeBodyPart() {
                @Override
                public void updateHeaders() throws MessagingException {
                    super.updateHeaders();
                    setHeader("Content-Transfer-Encoding", "8bit");
                }
            };
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
     * It might be worthwhile to use the mime-util library which does a much better job:
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
        Address sender = getSender();
        return sender==null || "Anonymous".equalsIgnoreCase(sender.toString());
    }
    
    /** Returns the value of the "from:" header, or <code>null</code> if there is none. */
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
     * Called by {@link saveChanges()}, see JavaMail JavaDoc.
     * @throws MessagingException
     */
    @Override
    public void updateHeaders() throws MessagingException {
        super.updateHeaders();
        scrubHeaders();
        removeRecipientNames();
        
        // Depending on includeSendTime, set the send time or remove the send time field
        if (includeSendTime) {
            if (getSentDate() == null) {
                // Set the "Date" field in UTC time, using the English locale.
                MailDateFormat formatter = new MailDateFormat();
                formatter.setTimeZone(TimeZone.getTimeZone("GMT"));   // always use UTC for outgoing mail
                setHeader("Date", formatter.format(new Date()));
            }
        }
        else
            removeHeader("Date");
    }

    /**
     * Creates a digital signature of the email and stores it in the
     * <code>SIGNATURE_HEADER</code> header field. It also removes the
     * <code>SIGNATURE_VALID_HEADER</code> header.
     * The signature is computed over the stream representation of the
     * email, minus the signature header if it is present.
     * The signature includes the ID number of the {@link CryptoImplementation}
     * used (signature lengths can be different for the same algorithm).
     * @param senderIdentity
     * @throws MessagingException
     * @throws GeneralSecurityException 
     */
    private void sign(EmailIdentity senderIdentity) throws MessagingException, GeneralSecurityException {
        removeHeader(SIGNATURE_HEADER);   // make sure there is no existing signature which would make the new signature invalid
        removeHeader(SIGNATURE_VALID_HEADER);   // remove the signature validity flag before signing
        CryptoImplementation cryptoImpl = senderIdentity.getCryptoImpl();
        PrivateKey signingKey = senderIdentity.getPrivateSigningKey();
        byte[] signature = cryptoImpl.sign(toByteArray(), signingKey);
        setHeader(SIGNATURE_HEADER, cryptoImpl.getId() + "_" + Base64.encode(signature));
    }
    
    /**
     * Verifies the signature and sets the <code>SIGNATURE_VALID_HEADER</code>
     * header field accordingly.
     */
    public void setSignatureFlag() {
        try {
            removeHeader(SIGNATURE_VALID_HEADER);   // remove the signature validity flag before verifying
            boolean valid = verifySignature();
            setHeader(SIGNATURE_VALID_HEADER, Boolean.valueOf(valid).toString());
        } catch (MessagingException e) {
            log.error("Cannot get header field: " + SIGNATURE_VALID_HEADER, e);
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
        
        // the crypto implementation ID is the number before the underscore
        int _index = signatureHeader.indexOf('_');
        if (_index < 0)
            return false;
        String cryptoImplIdString = signatureHeader.substring(0, _index);
        int cryptoImplId = 0;
        try {
            cryptoImplId = Integer.valueOf(cryptoImplIdString);
        }
        catch (NumberFormatException e) {
            return false;
        }
        CryptoImplementation cryptoImpl = CryptoFactory.getInstance(cryptoImplId);
        
        // the actual signature is everything after the underscore
        String base64Signature = signatureHeader.substring(_index + 1);
        try {
            removeHeader(SIGNATURE_HEADER);   // remove the signature before verifying
            byte[] signature = Base64.decode(base64Signature);
            EmailDestination senderDestination = new EmailDestination(getSender().toString());
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
     * Throws an exception if one or more address fields contain an invalid
     * Email Destination. If all addresses are valid, nothing happens.
     * @throws MessagingException
     * @throws DataFormatException
     */
    public void checkAddresses() throws MessagingException, DataFormatException {
        Collection<Header> headers = getAllAddressHeaders();
        for (Header header: headers) {
            String address = header.getValue();
            if (!"Sender".equalsIgnoreCase(header.getName()) || !isAnonymous()) {   // don't validate if this is the "sender" field and the sender is anonymous
                boolean validEmailDest = false;
                try {
                    new EmailDestination(address);
                    validEmailDest = true;
                }
                catch (GeneralSecurityException e) {
                    log.debug("Address contains no email destination: <" + address + ">, message: " + e.getLocalizedMessage());
                }
                try {
                    String adrString = address.toString();
                    new InternetAddress(adrString, true);
                    // InternetAddress accepts addresses without a domain, so check that there is a '.' after the '@'
                    if (adrString.indexOf('@') >= adrString.indexOf('.'))
                        throw new DataFormatException(_("Invalid address: {0}", address));
                    validEmailDest = true;
                } catch (AddressException e) {
                    log.debug("Address contains no external email address: <" + address + ">, message: " + e.getLocalizedMessage());
                }
                if (!validEmailDest)
                    throw new DataFormatException(_("Address doesn't contain an Email Destination or an external address: {0}", address));
            }
        }
    }
    
    public void fixAddresses() throws MessagingException {
        List<Header> addressHeaders = getAllAddressHeaders();
        for (String headerName: ADDRESS_HEADERS)
            removeHeader(headerName);
        for (Header header: addressHeaders) {
            String fixedAddress = Util.fixAddress(header.getValue());
            addHeader(header.getName(), fixedAddress);
        }
    }
    
    private List<Header> getAllAddressHeaders() throws MessagingException {
        @SuppressWarnings("unchecked")
        Enumeration<Header> addressHeaders = (Enumeration<Header>)getMatchingHeaders(ADDRESS_HEADERS);
        return Collections.list(addressHeaders);
    }
    
    /**
     * Returns all "Reply To" addresses (usually zero or one).
     * Unlike {@link #getReplyTo()}, this method does not return
     * the "From" address if there is no "Reply To" address.
     * @throws MessagingException
     */
    public String[] getReplyToAddresses() throws MessagingException {
        return getHeader("Reply-To");
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
        removeRecipientNames("To");
        removeRecipientNames("CC");
        removeRecipientNames("BCC");
    }
    
    private void removeRecipientNames(String headerName) throws MessagingException {
        String[] headerValues = getHeader(headerName);
        removeHeader(headerName);
        if (headerValues != null)
            for (String recipient: headerValues) {
                String dest = EmailDestination.extractBase64Dest(recipient);
                if (dest != null)
                    addHeader(headerName, dest);
                // If there is no email destination, assume it is an external address and don't change it
                else
                    addHeader(headerName, recipient);
            }
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
    
    /** @see EmailMetadata#setNew(boolean) */
    public void setNew(boolean isNew) {
        metadata.setNew(isNew);
    }

    /** @see EmailMetadata#isNew() */
    public boolean isNew() {
        return metadata.isNew();
    }

    /** @see EmailMetadata#setCreateTime(Date) */
    public void setCreateTime(Date createTime) {
        metadata.setCreateTime(createTime);
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
     * Updates headers, signs the email, and converts it into one or more email packets.
     * If an error occurs, an empty <code>Collection</code> is returned.
     *
     * @param senderIdentity The sender's Email Identity, or <code>null</code> for anonymous emails
     * @param bccToKeep All BCC fields in the header section of the email are removed, except this field. If this parameter is <code>null</code>, all BCC fields are written.
     * @param maxPacketSize The size limit in bytes
     * @throws MessagingException
     * @throws GeneralSecurityException If the email cannot be signed
     */
    public Collection<UnencryptedEmailPacket> createEmailPackets(EmailIdentity senderIdentity, String bccToKeep, int maxPacketSize) throws MessagingException, GeneralSecurityException {
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
                sign(senderIdentity);
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
     * @return
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
        lzmaEncoder.SetDictionarySize(1<<22);   // dictionary size = 4 MBytes
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
     * @return
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
     * @return
     * @throws MessagingException
     */
    private boolean isBCC(String address) throws MessagingException {
        String[] bccAddresses = getHeader("BCC");
        if (bccAddresses == null)
            return false;
        
        for (String bccAddress: bccAddresses)
            if (bccAddress.equals(address))
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
     * @return
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
            result = result.append("Sender: ").append(getSender());
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