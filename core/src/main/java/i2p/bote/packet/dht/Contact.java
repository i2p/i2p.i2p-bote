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

import i2p.bote.Util;
import i2p.bote.crypto.CryptoImplementation;
import i2p.bote.crypto.KeyUpdateHandler;
import i2p.bote.email.EmailDestination;
import i2p.bote.email.EmailIdentity;
import i2p.bote.email.Fingerprint;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.packet.TypeCode;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import net.i2p.data.Hash;
import net.i2p.util.Log;

import com.lambdaworks.codec.Base64;

/**
 * Represents an address book entry. Can be stored in the DHT.
 */
@TypeCode('C')
public class Contact extends DhtStorablePacket {
    private final static Charset UTF8 = Charset.forName("UTF-8");
    
    private Log log = new Log(Contact.class);
    private String name;
    private Hash nameHash;   // SHA-256 hash of the UTF8-encoded name in lower case
    private EmailDestination destination;
    private byte[] picture;
    private String text;
    private byte[] signature;
    private Fingerprint fingerprint;

    /**
     * Creates a new <code>Contact</code>. Calculates a salt value which takes some time;
     * also creates a signature.
     * 
     * @param identity The email identity associated with the name
     * @param keyUpdateHandler For signing the packet
     * @param picture A browser-renderable picture
     * @param text
     * @param fingerprint
     * @throws GeneralSecurityException
     * @throws PasswordException 
     */
    public Contact(EmailIdentity identity, KeyUpdateHandler keyUpdateHandler, byte[] picture, String text, Fingerprint fingerprint) throws GeneralSecurityException, PasswordException {
        destination = identity;
        this.picture = picture;
        this.text = text;
        this.fingerprint = fingerprint;
        nameHash = EmailIdentity.calculateHash(identity.getPublicName());
        sign(identity, keyUpdateHandler);
    }
    
    /**
     * @param name A name chosen by the user who created the directory entry
     * @param destination
     */
    public Contact(String name, EmailDestination destination) {
        this.name = name;
        this.destination = destination;
        nameHash = EmailIdentity.calculateHash(name);
    }
    
    public Contact(String name, EmailDestination destination, String pictureBase64, String text) {
        this.name = name;
        this.destination = destination;
        setPictureBase64(pictureBase64);
        this.text = text;
    }

    /** Restores a <code>Contact</code> from its byte array representation. Note that the <code>name</code> field is not set. */
    public Contact(byte[] data) throws GeneralSecurityException {
        super(data);
        ByteBuffer buffer = ByteBuffer.wrap(data, HEADER_LENGTH, data.length-HEADER_LENGTH);
        
        try {
            nameHash = readHash(buffer);
            
            int emailDestLength = buffer.getShort();
            byte[] emailDestBytes = new byte[emailDestLength];
            buffer.get(emailDestBytes);
            destination = new EmailDestination(emailDestBytes);
            
            byte[] salt = new byte[Fingerprint.NUM_SALT_BYTES];
            buffer.get(salt);
            fingerprint = new Fingerprint(nameHash, destination, salt);
            
            int pLen = buffer.getShort();
            picture = new byte[pLen];
            buffer.get(picture);
            
            byte compression = buffer.get();
            
            int tLen = buffer.getShort();
            byte[] utf8Bytes = new byte[tLen];
            buffer.get(utf8Bytes);
            text = new String(utf8Bytes, UTF8);
            
            int sigLen = buffer.getShort();
            signature = new byte[sigLen];
            buffer.get(signature);
        }
        catch (BufferUnderflowException e) {
            log.error("Not enough bytes in packet.", e);
        }
        
        if (buffer.hasRemaining())
            log.debug("Extra bytes in Directory Entry data.");
    }

    public Hash getNameHash() {
        return nameHash;
    }
    
    /**
     * Creates a signature over the byte array representation of the packet
     * @param identity An email identity that matches the destination field
     * @param keyUpdateHandler
     * @throws PasswordException 
     * @throws GeneralSecurityException 
     */
    private void sign(EmailIdentity identity, KeyUpdateHandler keyUpdateHandler) throws GeneralSecurityException, PasswordException {
        byte[] data = getDataToSign();
        CryptoImplementation cryptoImpl = identity.getCryptoImpl();
        PrivateKey privateSigningKey = identity.getPrivateSigningKey();
        signature = cryptoImpl.sign(data, privateSigningKey, keyUpdateHandler);
    }
    
    /**
     * Verifies the signature and the fingerprint.
     * @return
     * @throws GeneralSecurityException 
     */
    public boolean verify() throws GeneralSecurityException {
        if (signature==null || fingerprint==null)
            return false;
        
        CryptoImplementation cryptoImpl = destination.getCryptoImpl();
        PublicKey key = destination.getPublicSigningKey();
        boolean valid = cryptoImpl.verify(getDataToSign(), signature, key);
        
        valid &= fingerprint.isValid();
        return valid;
    }
    
    public Fingerprint getFingerprint() {
        return fingerprint;
    }
    
    private byte[] getDataToSign() {
        byte[] data = toByteArray();
        int sigLen = signature==null ? 2 : signature.length+2;   // the sig length and the 2 length bytes
        data = Arrays.copyOf(data, data.length-sigLen);
        return data;
    }
    
    public void setName(String name) {
        this.name = name;
        nameHash = EmailIdentity.calculateHash(name);
    }

    public String getName() {
        return name;
    }
    
    @Override
    public Hash getDhtKey() {
        return getNameHash();
    }

    public String getBase64Dest() {
        return destination.toBase64();
    }

    public EmailDestination getDestination() {
        return destination;
    }
    
    public void setPictureBase64(String pictureBase64) {
        if (pictureBase64 == null)
            picture = null;
        else
            picture = Base64.decode(pictureBase64.toCharArray());
    }
    
    /**
     * Returns the picture in <b>standard</b> base64 encoding
     * (not the modified I2P encoding so the browser understands it).
     */
    public String getPictureBase64() {
        if (picture == null)
            return null;
        return new String(Base64.encode(picture));
    }
    
    /** @see Util#getPictureType(byte[]) */
    public String getPictureType() {
        return Util.getPictureType(picture);
    }
    
    public void setText(String text) {
        this.text = text;
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
            
            dataStream.write(fingerprint.getSalt());
            
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
            
            if (signature == null)
                dataStream.writeShort(0);
            else {
                dataStream.writeShort(signature.length);
                dataStream.write(signature);
            }
        }
        catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream/DataOutputStream.", e);
        }
        return byteStream.toByteArray();
    }
}
