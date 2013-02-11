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
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.fileencryption.SCryptParameters;
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
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import net.i2p.I2PAppContext;
import net.i2p.crypto.SHA256Generator;
import net.i2p.data.Hash;
import net.i2p.util.Log;
import net.i2p.util.RandomSource;

import com.lambdaworks.codec.Base64;
import com.lambdaworks.crypto.SCrypt;

/**
 * Represents an address book entry. Can be stored in the DHT.
 */
@TypeCode('C')
public class Contact extends DhtStorablePacket {
    private final static Charset UTF8 = Charset.forName("UTF-8");
    private final static SCryptParameters SCRYPT_PARAMETERS = new SCryptParameters(1<<14, 8, 1);
    private final static int NUM_FINGERPRINT_BYTES = 32;   // length of the raw fingerprint
    private final static int NUM_WORDS_IN_FINGERPRINT = 7;   // #words generated from the raw fingerprint
    private final static int NUM_WORDS_IN_LIST = 1 << 13;   // must be a power of 2
    private final static int NUM_SALT_BYTES = 4;
    
    private Log log = new Log(Contact.class);
    private String name;
    private Hash nameHash;   // SHA-256 hash of the UTF8-encoded name in lower case
    private EmailDestination destination;
    private byte[] salt;
    private byte[] picture;
    private String text;
    private byte[] signature;

    /**
     * Creates a new <code>Contact</code>. Calculates a salt value which takes some time;
     * also creates a signature.
     * 
     * @param identity The email identity associated with the name
     * @param keyUpdateHandler For signing the packet
     * @param picture A browser-renderable picture
     * @param text
     * @throws GeneralSecurityException
     * @throws PasswordException 
     */
    public Contact(EmailIdentity identity, KeyUpdateHandler keyUpdateHandler, byte[] picture, String text) throws GeneralSecurityException, PasswordException {
        this(calculateHash(identity.getPublicName()), identity, picture, text);
        generateSalt();
        sign(identity, keyUpdateHandler);
    }
    
    public Contact(Hash nameHash, EmailDestination destination, byte[] picture, String text) {
        this.nameHash = nameHash;
        this.destination = destination;
        this.picture = picture;
        this.text = text;
    }
    
    /**
     * @param name A name chosen by the user who created the directory entry
     * @param destination
     */
    public Contact(String name, EmailDestination destination) {
        this.name = name;
        nameHash = calculateHash(name);
        this.destination = destination;
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
            
            salt = new byte[NUM_SALT_BYTES];
            buffer.get(salt);
            
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

    /** Returns the DHT key */
    public static Hash calculateHash(String name) {
        name = name.trim();
        if (name.endsWith("@bote.i2p"))
            name = name.substring(0, name.length()-"@bote.i2p".length());
        byte[] nameBytes = name.toLowerCase().getBytes(UTF8);
        return SHA256Generator.getInstance().calculateHash(nameBytes);
    }
    
    /** Finds a salt value such that scrypt(nameHash|destination, salt) starts with a zero byte */
    private void generateSalt() throws GeneralSecurityException {
        byte[] input = Util.concat(nameHash.toByteArray(), destination.toByteArray());
        RandomSource randomSource = I2PAppContext.getGlobalContext().random();
        while (true) {
            salt = new byte[NUM_SALT_BYTES];
            randomSource.nextBytes(salt);
            byte[] fingerprint = SCrypt.scrypt(input, salt, SCRYPT_PARAMETERS.N, SCRYPT_PARAMETERS.r, SCRYPT_PARAMETERS.p, NUM_FINGERPRINT_BYTES);
            if (fingerprint[31] == 0)
                return;
        }
    }
    
    /**
     * Returns a byte array of length <code>NUM_FINGERPRINT_BYTES</code>.
     * @return
     * @throws GeneralSecurityException
     */
    private byte[] getRawFingerprint() throws GeneralSecurityException {
        byte[] input = Util.concat(nameHash.toByteArray(), destination.toByteArray());
        byte[] fingerprint = SCrypt.scrypt(input, salt, SCRYPT_PARAMETERS.N, SCRYPT_PARAMETERS.r, SCRYPT_PARAMETERS.p, NUM_FINGERPRINT_BYTES);
        return fingerprint;
    }
    
    /**
     * Returns the fingerprint for this <code>Contact</code> based on a word list.<br/>
     * Throws a <code>NullPointerException</code> if salt is not initialized.<br/>
     * This fingerprint doesn't contain the full <code>NUM_FINGERPRINT_BYTES</code> bytes
     * worth of information from the raw fingerprint, but it is more recognizable to users.
     * @param wordList a word list for some locale
     * @throws GeneralSecurityException
     */
    public String getFingerprint(String[] wordList) throws GeneralSecurityException {
        byte[] fingerprintBytes = getRawFingerprint();
        ByteBuffer fingerprintBuffer = ByteBuffer.wrap(fingerprintBytes);
        StringBuilder fingerprintWords = new StringBuilder();
        for (int i=0; i<NUM_WORDS_IN_FINGERPRINT; i++) {
            if (i > 0)
                fingerprintWords.append(" - ");
            int index = fingerprintBuffer.getShort() & (NUM_WORDS_IN_LIST - 1);
            String word = wordList[index];
            fingerprintWords.append(word);
        }
        
        return fingerprintWords.toString();
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
     * Verifies the signature and checks that the 31th byte of the fingerprint is zero.
     * @return
     * @throws GeneralSecurityException 
     */
    public boolean verify() throws GeneralSecurityException {
        CryptoImplementation cryptoImpl = destination.getCryptoImpl();
        PublicKey key = destination.getPublicSigningKey();
        boolean valid = cryptoImpl.verify(getDataToSign(), signature, key);
        
        valid &= getRawFingerprint()[NUM_FINGERPRINT_BYTES-1] == 0;
        return valid;
    }
    
    private byte[] getDataToSign() {
        byte[] data = toByteArray();
        int sigLen = signature==null ? 2 : signature.length+2;   // the sig length and the 2 length bytes
        data = Arrays.copyOf(data, data.length-sigLen);
        return data;
    }
    
    public void setName(String name) {
        this.name = name;
        nameHash = calculateHash(name);
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
            
            dataStream.write(salt);
            
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