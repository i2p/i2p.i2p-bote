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
import i2p.bote.I2PBote;
import i2p.bote.UniqueId;
import i2p.bote.Util;
import i2p.bote.crypto.CryptoFactory;
import i2p.bote.crypto.CryptoImplementation;
import i2p.bote.packet.UnencryptedEmailPacket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import net.i2p.data.Base64;
import net.i2p.data.DataFormatException;
import net.i2p.util.Log;

import com.nettgryppa.security.HashCash;

public class Email extends MimeMessage {
    private static final int MAX_BYTES_PER_PACKET = 30 * 1024;
    private static final String SIGNATURE_HEADER = "X-I2PBote-Signature";   // contains the sender's base64-encoded signature
    private static final String SIGNATURE_VALID_HEADER = "X-I2PBote-Sig-Valid";   // contains the string "true" or "false"
    private static final String[] HEADER_WHITELIST = new String[] {
        "From", "Sender", "To", "CC", "BCC", "Reply-To", "Subject", "Date", "MIME-Version", "Content-Type",
        "Content-Transfer-Encoding", "In-Reply-To", "X-HashCash", "X-Priority", SIGNATURE_HEADER
    };
    private static final String[] ADDRESS_HEADERS = new String[] {"From", "Sender", "To", "CC", "BCC", "Reply-To"};
    
    private static Log log = new Log(Email.class);
    private UniqueId messageId;
    private boolean isNew = true;

    public Email() {
        super(Session.getDefaultInstance(new Properties()));
        messageId = new UniqueId();
    }

    public Email(File file) throws FileNotFoundException, MessagingException {
        this(new FileInputStream(file));
    }
    
    /**
     * Creates an Email object from an InputStream containing a MIME email.
     * 
     * @param inputStream
     * @throws MessagingException 
     */
    private Email(InputStream inputStream) throws MessagingException {
        super(Session.getDefaultInstance(new Properties()), inputStream);
        messageId = new UniqueId();
    }

   /**
    * Creates an Email object from a byte array containing a MIME email.
    * 
    * @param bytes
    * @throws MessagingException 
    */
    public Email(byte[] bytes) throws MessagingException {
        super(Session.getDefaultInstance(new Properties()), new ByteArrayInputStream(bytes));
        messageId = new UniqueId();
    }

    /**
     * Returns <code>true</code> if the sender is anonymous.
     * @return
     * @throws MessagingException
     */
    public boolean isAnonymous() throws MessagingException {
        Address sender = getSender();
        return sender==null || "Anonymous".equalsIgnoreCase(sender.toString());
    }
    
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
     * basic header fields.
     * Called by <code>saveChanges()</code>, see JavaMail JavaDoc.
     * @throws MessagingException
     */
    @Override
    public void updateHeaders() throws MessagingException {
        super.updateHeaders();
        scrubHeaders();
        removeRecipientNames();
        
        // Set the send time if the "Include sent time" config setting is enabled;
        // otherwise, remove the send time field.
        if (I2PBote.getInstance().getConfiguration().getIncludeSentTime()) {
            // Set the "Date" field in UTC time, using the English locale.
            long currentTime = new Date().getTime();
            long timeZoneOffset = TimeZone.getDefault().getOffset(currentTime);
            currentTime -= timeZoneOffset;
            DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss +0000", Locale.ENGLISH);   // always use UTC for outgoing mail
            setHeader("Date", formatter.format(currentTime));
        }
        else
            removeHeader("Date");
    }

    /**
     * Creates a digital signature of the email and stores it in the
     * <code>SIGNATURE_HEADER</code> header field. It also removes the
     * <code>SIGNATURE_VALID_FLAG</code> header.
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
     * Verifies the signature and sets the <code>SIGNATURE_VALID_FLAG</code>
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
     * Verifies that the email contains a valid signature. If the
     * <code>SIGNATURE_VALID_FLAG</code> is present, its value is used.
     * If not, the value of the <code>SIGNATURE_HEADER</code> header
     * field is verified (which is more CPU intensive).
     * @return
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
     * contains a valid signature.
     * @return
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
            removeHeader(SIGNATURE_HEADER);
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
     * Unlike {@link getReplyTo()}, this method does not return
     * the "From" address if there is no "Reply To" address.
     * @return
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
    
    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    /**
     * Returns <code>true</code> if the email is unread (incoming mail), or
     * if it has not been sent yet (outgoing mail).
     * @return
     */
    public boolean isNew() {
        return isNew;
    }

    public String getText() {
        try {
            return getContent().toString();
        } catch (Exception e) {
            String errorMsg = "Error reading email content.";
            log.error(errorMsg, e);
            return errorMsg;
        }
    }
    
    /**
     * Updates headers, signs the email, and converts it into one or more email packets.
     * If an error occurs, an empty <code>Collection</code> is returned.
     *
     * @param senderIdentity The sender's Email Identity, or <code>null</code> for anonymous emails
     * @param bccToKeep All BCC fields in the header section of the email are removed, except this field. If this parameter is <code>null</code>, all BCC fields are written.
     * @return
     * @throws MessagingException
     * @throws GeneralSecurityException 
     */
    public Collection<UnencryptedEmailPacket> createEmailPackets(EmailIdentity senderIdentity, String bccToKeep) throws MessagingException, GeneralSecurityException {
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
            writeTo(outputStream);
        } catch (IOException e) {
            throw new MessagingException("Can't write to ByteArrayOutputStream.", e);
        } catch (GeneralSecurityException e) {
            throw new GeneralSecurityException("Can't sign email.", e);
        } finally {
            // restore the BCC headers
            removeHeader("BCC");
            if (bccHeaders != null)
                for (String bccAddress: bccHeaders)
                    addHeader("BCC", bccAddress);
        }
        byte[] emailArray = outputStream.toByteArray();
        
        // calculate packet count
        int numPackets = (emailArray.length+MAX_BYTES_PER_PACKET-1) / MAX_BYTES_PER_PACKET;
        
        int packetIndex = 0;
        int blockStart = 0;   // the array index where the next block of data starts
        while (true) {
            int blockSize = Math.min(emailArray.length-blockStart, MAX_BYTES_PER_PACKET);
            if (blockSize <= 0)
                break;
            else {
                // make a new array with the right length
                byte[] block = new byte[blockSize];
                System.arraycopy(emailArray, blockStart, block, 0, blockSize);
                UnencryptedEmailPacket packet = new UnencryptedEmailPacket(messageId, packetIndex, numPackets, block);
                packets.add(packet);
                packetIndex++;
                blockStart += blockSize;
            }
        }
        
        return packets;
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