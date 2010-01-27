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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import net.i2p.client.I2PClient;
import net.i2p.client.I2PClientFactory;
import net.i2p.client.I2PSession;
import net.i2p.crypto.SHA256Generator;
import net.i2p.data.Base64;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.data.PublicKey;
import net.i2p.data.SigningPublicKey;
import net.i2p.util.Log;

/**
 * Uniquely identifies an email recipient. This implementation uses I2P keypairs.
 */
public class EmailDestination {
    private Log log = new Log(EmailDestination.class);
    private PublicKey publicEncryptionKey;
    private SigningPublicKey publicSigningKey;
    
    /**
     * Creates a fresh <code>EmailDestination</code>.
     */
    public EmailDestination() {
        try {
            I2PClient i2pClient = I2PClientFactory.createClient();
            ByteArrayOutputStream arrayStream = new ByteArrayOutputStream();
            i2pClient.createDestination(arrayStream);
            byte[] destinationArray = arrayStream.toByteArray();
            I2PSession i2pSession = i2pClient.createSession(new ByteArrayInputStream(destinationArray), null);
            
            initKeys(i2pSession);
        }
        catch (Exception e) {
            log.error("Can't generate EmailDestination.", e);
        }
    }

    /**
     * Creates a <code>EmailDestination</code> using data read from a {@link ByteBuffer}.
     * @param buffer
     */
    public EmailDestination(ByteBuffer buffer) {
        byte[] encryptionKeyArray = new byte[PublicKey.KEYSIZE_BYTES];
        buffer.get(encryptionKeyArray);
        publicEncryptionKey = new PublicKey(encryptionKeyArray);
        
        byte[] signingKeyArray = new byte[SigningPublicKey.KEYSIZE_BYTES];
        buffer.get(signingKeyArray);
        publicSigningKey = new SigningPublicKey(signingKeyArray);
    }
    
    /**
     * @param address A string containing a valid base64-encoded Email Destination
     * @throws DataFormatException If <code>address</code> doesn't contain a valid Email Destination
     */
    public EmailDestination(String address) throws DataFormatException {
        String base64Data = extractBase64Dest(address);
        if (base64Data == null) {
            String msg = "No Email Destination found in string: <" + address + ">";
            log.debug(msg);
            throw new DataFormatException(msg);
        }
        
        base64Data += "AAAA";   // add a null certificate
        Destination i2pDestination = new Destination(base64Data);
        publicEncryptionKey = i2pDestination.getPublicKey();
        publicSigningKey = i2pDestination.getSigningPublicKey();
    }
    
    /**
     * Looks for a Base64-encoded Email Destination in a string. Returns
     * the 512-byte Base64 string, or <code>null</code> if nothing is found.
     * Even if the return value is non-<code>null</code>, it is not
     * guaranteed to be a valid Email Destination.
     * @param address
     * @return
     */
    private String extractBase64Dest(String address) {
        if (address==null || address.length()<512)
            return null;
        
        if (address.length() == 512)
            return address;
        
        // Check if the string contains 512 chars in angle brackets
        int bracketIndex = address.indexOf('<');
        if (bracketIndex>=0 && address.length()>bracketIndex+512)
            return address.substring(bracketIndex+1, bracketIndex+1+512);
        
        // Check if the string is of the form EmailDest@foo
        if (address.indexOf('@') == 512)
            return address.substring(0, 513);
        
        return null;
    }
    
    protected void initKeys(I2PSession i2pSession) {
        publicEncryptionKey = i2pSession.getMyDestination().getPublicKey();
        publicSigningKey = i2pSession.getMyDestination().getSigningPublicKey();
    }
    
    public PublicKey getPublicEncryptionKey() {
        return publicEncryptionKey;
    }

    public SigningPublicKey getPublicSigningKey() {
        return publicSigningKey;
    }
    
    private byte[] getKeysAsArray() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try {
            writeTo(byteStream);
        }
        catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
        }
        return byteStream.toByteArray();
    }
    
    private void writeTo(OutputStream outputStream) throws IOException {
        try {
            publicEncryptionKey.writeBytes(outputStream);
            publicSigningKey.writeBytes(outputStream);
        }
        catch (DataFormatException e) {
            log.error("Invalid encryption key or signing key.", e);
        }
    }
    
    public Hash getHash() {
        // TODO cache the hash value?
        return SHA256Generator.getInstance().calculateHash(getKeysAsArray());
    }
    
    /**
     * Returns the two public keys in Base64 representation.
     */
    public String getKey() {
        return Base64.encode(getKeysAsArray());
    }
    
    public String toBase64() {
        return getKey();
    }
    
    @Override
    public String toString() {
        return getKey();
    }
}