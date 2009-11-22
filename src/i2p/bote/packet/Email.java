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

package i2p.bote.packet;

import i2p.bote.RecipientType;
import i2p.bote.Util;
import i2p.bote.folder.FolderElement;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.i2p.util.ConcurrentHashSet;
import net.i2p.util.Log;

import com.nettgryppa.security.HashCash;

// TODO move one package up
public class Email implements FolderElement {
    private static final int MAX_BYTES_PER_PACKET = 30 * 1024;
    private static final byte[] NEW_LINE = new byte[] {13, 10};   // separates header section from mail body; same for all platforms per RFC 5322
    private static final Set<String> HEADER_WHITELIST = createHeaderWhitelist();
    
    private static Log log = new Log(Email.class);
    private File file;
    private List<Header> headers;
    private byte[] content;
    private UniqueId messageId;

    public Email() {
        headers = Collections.synchronizedList(new ArrayList<Header>());
        content = new byte[0];
        messageId = new UniqueId();
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
        InputStream inputStream = new ByteArrayInputStream(bytes);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        
        headers = Collections.synchronizedList(new ArrayList<Header>());
        try {
            // read mail headers
            while (true) {
                String line = reader.readLine();
                if (line == null)   // EOF
                    break;
                if ("".equals(line))   // empty line separates header from mail body
                    break;
                
                String[] splitString = line.split(":\\s", 1);
                String name = splitString[0];
                String value = splitString.length>=2 ? splitString[1] : "";
                if (HEADER_WHITELIST.contains(name))
                    headers.add(new Header(name, value));
            }
        
            // read body
            content = Util.readInputStream(inputStream);
        }
        catch (IOException e) {
            log.error("Can't read from ByteArrayInputStream.", e);
        }
        
        messageId = new UniqueId();
    }

    private static Set<String> createHeaderWhitelist() {
        String[] headerArray = new String[] {
            "From", "Sender", "To", "CC", "BCC", "Reply-To", "Subject", "MIME-Version", "Content-Type", "Content-Transfer-Encoding",
            "Message-Id", "In-Reply-To", "X-HashCash"
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
    
    public void setContent(String content) {
        this.content = content.getBytes();
    }
    
    public void setContent(byte[] content) {
        this.content = content;
    }
    
    public byte[] getContent() {
        return content;
    }
    
    /**
     * Returns a message ID that conforms to RFC5322
     * @return
     */
    public String getMessageID() {
        return messageId.toBase64() + "@i2p";
    }

    /**
     * Converts the email into one or more email packets.
     * 
     * @param bccToKeep All BCC fields in the header section of the email are removed, except this field 
     * @return
     * @throws IOException
     */
    public Collection<UnencryptedEmailPacket> createEmailPackets(String bccToKeep) {
        ArrayList<UnencryptedEmailPacket> packets = new ArrayList<UnencryptedEmailPacket>();
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
        	writeTo(outputStream, bccToKeep);
        }
        catch (IOException e) {
        	log.error("Can't write to ByteArrayOutputStream.", e);
        }
        byte[] emailArray = outputStream.toByteArray();
        
        // calculate fragment count
        int numFragments = (emailArray.length+MAX_BYTES_PER_PACKET-1) / MAX_BYTES_PER_PACKET;
        for (UnencryptedEmailPacket packet: packets)
            packet.setNumFragments(numFragments);
        
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
                UniqueId deletionKeyPlain = new UniqueId();
                UniqueId deletionKeyEncrypted = new UniqueId(deletionKeyPlain);   // encryption happens in the constructor call below
                UnencryptedEmailPacket packet = new UnencryptedEmailPacket(deletionKeyPlain, deletionKeyEncrypted, messageId, fragmentIndex, numFragments, block);
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

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        writeHeaders(outputStream);
        outputStream.write(NEW_LINE);
        outputStream.write(content);
    }
    
    public void writeTo(OutputStream outputStream, String bccToKeep) throws IOException {
        writeHeaders(outputStream, bccToKeep);
        outputStream.write(NEW_LINE);
        outputStream.write(content);
    }
    
    private void writeHeaders(OutputStream outputStream) throws IOException {
        PrintWriter writer = new PrintWriter(outputStream);
        for (Header header: headers)
            writer.write(header.toString());
    }
    
    private void writeHeaders(OutputStream outputStream, String bccToKeep) throws IOException {
        PrintWriter writer = new PrintWriter(outputStream);
        for (Header header: headers)
            if (!"BCC".equals(header.name) || bccToKeep.equals(header.value))
                writer.write(header.toString());
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