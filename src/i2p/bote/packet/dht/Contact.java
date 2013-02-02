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

package i2p.bote.packet.dht;

import i2p.bote.email.EmailDestination;
import i2p.bote.email.EmailIdentity;
import i2p.bote.packet.TypeCode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;

import net.i2p.crypto.SHA256Generator;
import net.i2p.data.Hash;
import net.i2p.util.Log;

import com.lambdaworks.codec.Base64;

/**
 * Represents an address book entry. Can be stored in the DHT.
 */
@TypeCode('C')
public class Contact extends DhtStorablePacket {
    private static Charset UTF8 = Charset.forName("UTF-8");
    
    private Log log = new Log(Contact.class);
    private String name;
    private Hash nameHash;   // SHA-256 hash of the UTF8-encoded name in lower case
    private EmailDestination destination;
    private byte[] picture;
    private String text;

    /**
     * 
     * @param name A name chosen by the user who created the directory entry
     * @param emailDestination The email destination associated with the name
     * @param picture A browser-renderable picture
     * @param text
     */
    public Contact(EmailIdentity identity, byte[] picture, String text) {
        this(calculateHash(identity.getPublicName()), identity, picture, text);
    }
    
    public Contact(Hash nameHash, EmailDestination destination, byte[] picture, String text) {
        this.nameHash = nameHash;
        this.destination = destination;
        this.picture = picture;
        this.text = text;
    }
    
    public Contact(String name, EmailDestination destination) {
        this.name = name;
        nameHash = calculateHash(name);
        this.destination = destination;
    }
    
    public static Hash calculateHash(String name) {
        if (name.endsWith(".bote.i2p"))
            name = name.substring(0, name.length()-".bote.i2p".length());
        byte[] nameBytes = name.toLowerCase().getBytes(UTF8);
        return SHA256Generator.getInstance().calculateHash(nameBytes);
    }
    
    public Contact(byte[] data) throws GeneralSecurityException {
        super(data);
        ByteBuffer buffer = ByteBuffer.wrap(data, HEADER_LENGTH, data.length-HEADER_LENGTH);
        
        try {
            nameHash = readHash(buffer);
            
            int emailDestLength = buffer.getShort();
            byte[] emailDestBytes = new byte[emailDestLength];
            buffer.get(emailDestBytes);
            destination = new EmailDestination(emailDestBytes);
            
            int pLen = buffer.getShort();
            picture = new byte[pLen];
            buffer.get(picture);
            
            byte compression = buffer.get();
            
            int tLen = buffer.getShort();
            byte[] utf8Bytes = new byte[tLen];
            buffer.get(utf8Bytes);
            text = new String(utf8Bytes, UTF8);
        }
        catch (BufferUnderflowException e) {
            log.error("Not enough bytes in packet.", e);
        }
        
        if (buffer.hasRemaining())
            log.debug("Extra bytes in Directory Entry data.");
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
    @Override
    public Hash getDhtKey() {
        return nameHash;
    }

    public String getBase64Dest() {
        return destination.toBase64();
    }

    public EmailDestination getDestination() {
        return destination;
    }
    
    /**
     * Returns the picture in <b>standard</b> base64 encoding
     * (not the modified I2P encoding so the browser understands it).
     */
    public String getPictureBase64() {
        return new String(Base64.encode(picture));
    }
    
    /** Returns the MIME type of the picture, for example <code>image/jpeg</code>. */
    public String getPictureType() {
        ByteArrayInputStream stream = new ByteArrayInputStream(picture);
        try {
            return URLConnection.guessContentTypeFromStream(stream);
        } catch (IOException e) {
            log.error("Can't read from ByteArrayInputStream", e);
            return null;
        }
    }
    
    /** Returns the text included with the directory entry. */
    public String getText() {
        return text;
    }
    
    @Override
    public byte[] toByteArray() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(byteStream);
        try {
            writeHeader(dataStream);
            
            dataStream.write(nameHash.toByteArray());
            
            byte[] destBytes = destination.toByteArray();
            dataStream.writeShort(destBytes.length);
            dataStream.write(destBytes);
            
            if (picture == null)
                picture = new byte[0];
            dataStream.writeShort(picture.length);
            dataStream.write(picture);
            
            if (text == null)
                text = "";
            byte[] utf8Text = text.getBytes(UTF8);
            dataStream.write(0);   // TODO compression type
            dataStream.writeShort(utf8Text.length);
            dataStream.write(utf8Text);
        }
        catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream/DataOutputStream.", e);
        }
        return byteStream.toByteArray();
    }
}