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

import i2p.bote.UniqueId;
import i2p.bote.packet.UnencryptedEmailPacket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeMessage;

import net.i2p.util.Log;

import com.nettgryppa.security.HashCash;

public class Email extends MimeMessage {
    private static final int MAX_BYTES_PER_PACKET = 30 * 1024;
    private static final String[] HEADER_WHITELIST = new String[] {
        "From", "Sender", "To", "CC", "BCC", "Reply-To", "Subject", "Date", "MIME-Version", "Content-Type",
        "Content-Transfer-Encoding", "In-Reply-To", "X-HashCash", "X-Priority"
    };
    
    private static Log log = new Log(Email.class);
    private UniqueId uniqueId;
    private boolean isNew = true;

    public Email() {
        super(Session.getDefaultInstance(new Properties()));
        uniqueId = new UniqueId();
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
        uniqueId = new UniqueId();
    }

   /**
    * Creates an Email object from a byte array containing a MIME email.
    * 
    * @param bytes
    * @throws MessagingException 
    */
    public Email(byte[] bytes) throws MessagingException {
        super(Session.getDefaultInstance(new Properties()), new ByteArrayInputStream(bytes));
        uniqueId = new UniqueId();
    }

    public void setHashCash(HashCash hashCash) throws MessagingException {
        setHeader("X-HashCash", hashCash.toString());
    }

    /**
     * Removes all headers that are not on the whitelist, and initializes some
     * basic header fields.
     * Called by <code>saveChanges()</code>, see JavaMail JavaDoc.
     */
    @Override
    public void updateHeaders() {
        try {
            scrubHeaders();
            setHeader("Content-Type", "text/plain");
            setHeader("Content-Transfer-Encoding", "7bit");
            
            // Set the "Date" field, using english for the locale.
            Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT+0"));
            DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss +0000", Locale.ENGLISH);   // always use UTC for outgoing mail
            setHeader("Date", formatter.format(calendar.getTime()));
        } catch (MessagingException e) {
            log.error("Cannot set mail headers.", e);
        }
    }

    /**
     * Creates a copy of the <code>Email</code> with all "BCC" headers removed, except the one for
     * <code>recipient<code>.
     * @String recipient
     * @return
     * @throws MessagingException 
     * @throws IOException 
     */
    private Email removeBCCs(String recipient) throws MessagingException, IOException {
        // make a copy of the email
        Email newEmail = new Email();
        newEmail.setContent(getContent(), getDataHandler().getContentType());
        
        // set new headers
        newEmail.headers = new InternetHeaders();
        @SuppressWarnings("unchecked")
        List<Header> headers = Collections.list(getAllHeaders());
        for (Header header: headers)
            if (!"BCC".equals(header.getName()) || !recipient.equals(header.getValue()))
                newEmail.addHeader(header.getName(), header.getValue());

        return newEmail;
    }
    
    /**
     * Removes all mail headers except the ones in <code>HEADER_WHITELIST</code>.
     * @throws MessagingException 
     */
    private void scrubHeaders() throws MessagingException {
        @SuppressWarnings("unchecked")
        List<Header> nonMatchingHeaders = Collections.list(getNonMatchingHeaders(HEADER_WHITELIST));
        for (Header header: nonMatchingHeaders) {
            log.debug("Removing all instances of non-whitelisted header <" + header.getName() + ">");
            removeHeader(header.getName());
        }
    }

    public void setUniqueId(UniqueId uniqueId) {
        setMessageId(uniqueId);
    }
    
    public void setMessageId(UniqueId uniqueId) {
        this.uniqueId = uniqueId;
        try {
            setHeader("Message-Id", uniqueId.toBase64() + "@i2p");
        }
        catch (MessagingException e) {
            log.error("Can't set message ID on email.", e);
        }
    }
    
    public UniqueId getUniqueID() {
        return uniqueId;
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
     * Converts the email into one or more email packets.
     * If an error occurs, an empty <code>Collection</code> is returned.
     * 
     * @param bccToKeep All BCC fields in the header section of the email are removed, except this field. If this parameter is <code>null</code>, all BCC fields are written.
     * @return
     */
    public Collection<UnencryptedEmailPacket> createEmailPackets(String bccToKeep) {
        ArrayList<UnencryptedEmailPacket> packets = new ArrayList<UnencryptedEmailPacket>();
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            saveChanges();
            if (bccToKeep == null)
                writeTo(outputStream);
            else
                removeBCCs(bccToKeep).writeTo(outputStream);
            
        } catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
            return packets;
        } catch (MessagingException e) {
            log.error("Can't remove BCC headers.", e);
            return packets;
        }
        byte[] emailArray = outputStream.toByteArray();
        
        // calculate fragment count
        int numFragments = (emailArray.length+MAX_BYTES_PER_PACKET-1) / MAX_BYTES_PER_PACKET;
        
        int fragmentIndex = 0;
        int blockStart = 0;   // the array index where the next block of data starts
        while (true) {
            int blockSize = Math.min(emailArray.length-blockStart, MAX_BYTES_PER_PACKET);
            if (blockSize <= 0)
                break;
            else {
                // make a new array with the right length
                byte[] block = new byte[blockSize];
                System.arraycopy(emailArray, blockStart, block, 0, blockSize);
                UniqueId deletionKey = new UniqueId();
                UnencryptedEmailPacket packet = new UnencryptedEmailPacket(uniqueId, fragmentIndex, numFragments, block, deletionKey);
                packets.add(packet);
                fragmentIndex++;
                blockStart += blockSize;
            }
        }
        
        return packets;
    }
}