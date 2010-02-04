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
import i2p.bote.Util;
import i2p.bote.folder.FolderElement;
import i2p.bote.packet.UnencryptedEmailPacket;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import net.i2p.util.ConcurrentHashSet;
import net.i2p.util.Log;

import com.nettgryppa.security.HashCash;

public class Email implements FolderElement {
    private static final int MAX_BYTES_PER_PACKET = 30 * 1024;
    private static final byte[] NEW_LINE = new byte[] {13, 10};   // separates header section from mail body; same for all platforms per RFC 5322
    private static final Set<String> HEADER_WHITELIST = createHeaderWhitelist();
    
    private static Log log = new Log(Email.class);
    private File file;
    private List<Header> headers;
    private byte[] content;   // save memory by using bytes rather than chars
    private UniqueId messageId;
    private boolean isNew;

    public Email() {
        headers = Collections.synchronizedList(new ArrayList<Header>());
        content = new byte[0];
        messageId = new UniqueId();
        isNew = true;
    }

    /**
     * Creates an Email object from an InputStream containing a MIME email.
     * 
     * @param inputStream
     * @throws IOException 
     */
    public Email(InputStream inputStream) throws IOException {
        this(Util.readInputStream(inputStream));
    }

   /**
    * Creates an Email object from a byte array containing a MIME email.
    * 
    * @param bytes
    */
    public Email(byte[] bytes) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes)));
        
        headers = Collections.synchronizedList(new ArrayList<Header>());
        ByteArrayOutputStream contentStream = new ByteArrayOutputStream();
        boolean allHeadersRead = false;
        try {
            // read mail headers
            while (true) {
                String line = reader.readLine();
                if (line == null)   // EOF
                    break;
                if ("".equals(line) && !allHeadersRead) {   // empty line separates header from mail body
                    allHeadersRead = true;
                    continue;
                }
                
                if (!allHeadersRead) {
                    String[] splitString = line.split(":\\s*", 2);
                    if (splitString.length > 1) {
                        String name = splitString[0];
                        String value = splitString[1];
                        if (HEADER_WHITELIST.contains(name))
                            headers.add(new Header(name, value));
                    }
                    else
                        allHeadersRead = true;
                }
                
                if (allHeadersRead) {
                    if (contentStream.size() > 0)
                        contentStream.write(NEW_LINE);
                    contentStream.write(line.getBytes());
                }
            }
        }
        catch (IOException e) {
            log.error("Can't read from ByteArrayInputStream.", e);
        }
        content = contentStream.toByteArray();
        
        isNew = true;
    }

    private static Set<String> createHeaderWhitelist() {
        String[] headerArray = new String[] {
            "From", "Sender", "To", "CC", "BCC", "Reply-To", "Subject", "Date", "MIME-Version", "Content-Type",
            "Content-Transfer-Encoding", "In-Reply-To", "X-HashCash", "X-Priority"
        };
        
        ConcurrentHashSet<String> headerSet = new ConcurrentHashSet<String>();
        headerSet.addAll(Arrays.asList(headerArray));
        return headerSet;
    }
    
    public void setHashCash(HashCash hashCash) {
        setHeader("X-HashCash", hashCash.toString());
    }

    public void setHeader(String name, String value) {
        for (Header header: headers)
            if (name.equals(header.name))
                headers.remove(header);
        addHeader(name, value);
    }
    
    public void addHeader(String name, String value) {
        if (HEADER_WHITELIST.contains(name))
            headers.add(new Header(name, value));
        else
            log.debug("Ignoring non-whitelisted header: " + name);
    }
    
    public void setSender(String sender) {
        setHeader("From", sender);
        setHeader("Sender", sender);
    }
    
    /**
     * Returns the value of the RFC 5322 "From" header field. If the "From" header
     * field is absent, the value of the "Sender" field is returned. If both
     * fields are absent, <code>null</code> is returned.
     * @return
     */
    public String getSender() {
        String sender = getHeader("From");
        if (sender != null)
            return sender;
        sender = getHeader("Sender");
        return sender;
    }
    
    public void setSubject(String subject) {
        setHeader("Subject", subject);
    }
    
    public String getSubject() {
        return getHeader("Subject");
    }
    
    public Collection<String> getAllRecipients() {
        List<String> recipients = new ArrayList<String>();
        for (Header header: headers)
            if (isRecipient(header.name))
                recipients.add(header.value);
        return recipients;
    }
    
    private boolean isRecipient(String headerName) {
        return RecipientType.TO.equalsString(headerName) || RecipientType.CC.equalsString(headerName) || RecipientType.BCC.equalsString(headerName);
    }
    
    public void addRecipient(RecipientType type, String address) {
        addHeader(type.toString(), address);
    }

    /**
     * Initializes some basic header fields.
     */
    public void updateHeaders() {
        setHeader("Content-Type", "text/plain");
        setHeader("Content-Transfer-Encoding", "7bit");
        
        // Set the "Date" field, using english for the locale.
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT+0"));
        DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss +0000", Locale.ENGLISH);   // always use UTC for outgoing mail
        setHeader("Date", formatter.format(calendar.getTime()));
    }
    
    /**
     * Returns the value of the "Date" header field.
     * @return
     */
    public String getDateString() {
        return getHeader("Date");
    }
    
    /**
     * Parses the value of the "Date" header field into a {@link Date}.
     * If the field cannot be parsed, <code>null</code> is returned.
     * @return
     */
    public Date getDate() {
        try {
            return parseDate(getDateString());
        }
        catch (ParseException e) {
            return null;
        }
    }
    
    /**
     * Example for a valid date string: Sat, 19 Aug 2006 20:05:41 +0100
     * @param dateString
     * @return
     * @throws ParseException
     */
    private Date parseDate(String dateString) throws ParseException {
        // remove day of week if present
        String[] tokens = dateString.split(",\\s+", 2);
        if (tokens.length > 1)
            dateString = tokens[1];

        DateFormat parser = new SimpleDateFormat("dd MMM yyyy kk:mm:ss Z", Locale.ENGLISH);
        return parser.parse(dateString);
    }
    
    public void setContent(String content) {
        this.content = content.getBytes();
    }
    
    public void setContent(byte[] content) {
        this.content = content;
    }
    
    public byte[] getContent() {
        return content;
    }
    
    public String getBodyText() {
        return new String(content);
    }
    
    public void setMessageId(UniqueId messageId) {
        this.messageId = messageId;
    }
    
    public UniqueId getMessageID() {
        return messageId;
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

    /**
     * Converts the email into one or more email packets.
     * 
     * @param bccToKeep All BCC fields in the header section of the email are removed, except this field. If this parameter is <code>null</code>, all BCC fields are written.
     * @return
     * @throws IOException
     */
    public Collection<UnencryptedEmailPacket> createEmailPackets(String bccToKeep) {
        ArrayList<UnencryptedEmailPacket> packets = new ArrayList<UnencryptedEmailPacket>();
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            writeTo(outputStream, bccToKeep);
        } catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
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
                UnencryptedEmailPacket packet = new UnencryptedEmailPacket(messageId, fragmentIndex, numFragments, block, deletionKey);
                packets.add(packet);
                fragmentIndex++;
                blockStart += blockSize;
            }
        }
        
        return packets;
    }

    /**
     * Removes all "BCC" headers except the one for <code>recipient<code>.
     * The mail body is not copied (which means the new email shares its body with the original).
     * @String recipient
     * @return
     */
    public Email removeBCCs(String recipient) {
        List<Header> newHeaders = Collections.synchronizedList(new ArrayList<Header>());
        for (Header header: headers)
            if (!"BCC".equals(header.name) || !recipient.equals(header.value))
                newHeaders.add(header);
        
        Email newEmail = new Email();
        newEmail.headers = newHeaders;
        newEmail.content = content;
        newEmail.messageId = messageId;
        
        return newEmail;
    }

    /**
     * Returns the value of the first header field for a header field name,
     * or <code>null</code> if no field by that name exists.
     * @param name
     * @return
     */
    private String getHeader(String name) {
        for (Header header: headers)
            if (name.equals(header.name))
                return header.value;
        return null;
    }
    
    // FolderElement implementation
    @Override
    public File getFile() {
    	return file;
    }

    // FolderElement implementation
    @Override
    public void setFile(File file) {
    	this.file = file;
    }

    /**
     * Writes the email as an RFC 5322 stream.
     * @see <a href="http://tools.ietf.org/html/rfc5322">http://tools.ietf.org/html/rfc5322</a>
     */
    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        writeTo(outputStream, null);
    }

    /**
     * Writes the email as an RFC 5322 stream.
     * @see <a href="http://tools.ietf.org/html/rfc5322">http://tools.ietf.org/html/rfc5322</a>
     * @param outputStream
     * @param bccToKeep All BCC fields in the header section of the email are removed, except this field. If this parameter is <code>null</code>, all BCC fields are written.
     * @throws IOException
     */
    private void writeTo(OutputStream outputStream, String bccToKeep) throws IOException {
        writeHeaders(outputStream, bccToKeep);
        outputStream.write(NEW_LINE);
        outputStream.write(content);
    }
    
    private void writeHeaders(OutputStream outputStream, String bccToKeep) throws IOException {
        for (Header header: headers)
            if (bccToKeep==null || !"BCC".equals(header.name) || bccToKeep.equals(header.value)) {
                outputStream.write(header.toString().getBytes());
                outputStream.write(NEW_LINE);
            }
    }
    
    private class Header {
        String name;
        String value;
        
        Header(String name, String value) {
            this.name = name;
            this.value = value;
        }
        
        @Override
        public String toString() {
            return name + ": " + value;
        }
    }
}