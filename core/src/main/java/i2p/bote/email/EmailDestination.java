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

import i2p.bote.crypto.CryptoFactory;
import i2p.bote.crypto.CryptoImplementation;
import i2p.bote.crypto.PublicKeyPair;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.util.Arrays;

import net.i2p.crypto.SHA256Generator;
import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * an <code>EmailDestination</code> uniquely identifies an email recipient. It consists
 * of a public encryption key and a public signing key.<br/>
 * The {@link CryptoImplementation} used by an Email Destination is not explicitly
 * encoded in the base64 representation. It is determined by the length of the base64 string.
 */
public class EmailDestination {
    private Log log = new Log(EmailDestination.class);
    protected CryptoImplementation cryptoImpl;
    protected PublicKey publicEncryptionKey;
    protected PublicKey publicSigningKey;
    
    protected EmailDestination() {
    }
    
    /**
     * @param address A string containing a valid base64-encoded Email Destination
     * @throws GeneralSecurityException If <code>address</code> doesn't contain a valid Email Destination
     */
    public EmailDestination(String address) throws GeneralSecurityException {
        String base64Key = extractBase64Dest(address);
        if (base64Key == null)
            throw new GeneralSecurityException("No Email Destination found in string: <" + address + ">");
        
        // find the crypto implementation for this key length
        for (CryptoImplementation cryptoImpl: CryptoFactory.getInstances()) {
            int base64Length = cryptoImpl.getBase64PublicKeyPairLength();   // length of an email destination that uses this CryptoImplementation
            if (base64Key.length() == base64Length)
                this.cryptoImpl = cryptoImpl;
        }
        if (cryptoImpl == null)
            throw new InvalidKeyException("Not a valid Email Destination: <" + base64Key + ">");
        
        PublicKeyPair keyPair = cryptoImpl.createPublicKeyPair(base64Key);
        publicEncryptionKey = keyPair.encryptionKey;
        publicSigningKey = keyPair.signingKey;
    }
    
    public EmailDestination(byte[] bytes) throws GeneralSecurityException {
        // find the crypto implementation for this key length
        for (CryptoImplementation cryptoImpl: CryptoFactory.getInstances()) {
            int byteArrayLength = cryptoImpl.getByteArrayPublicKeyPairLength();   // length of an email destination that uses this CryptoImplementation
            if (bytes.length == byteArrayLength)
                this.cryptoImpl = cryptoImpl;
        }
        if (cryptoImpl == null)
            throw new InvalidKeyException("Not a valid Email Destination: " + Arrays.toString(bytes));
        
        PublicKeyPair keyPair = cryptoImpl.createPublicKeyPair(bytes);
        publicEncryptionKey = keyPair.encryptionKey;
        publicSigningKey = keyPair.signingKey;
    }
    
    /**
     * Looks for a Base64-encoded Email Destination in a string. Returns
     * the Base64 encoding, or <code>null</code> if nothing is found.
     * Even if the return value is non-<code>null</code>, it is not
     * guaranteed to be a valid Email Destination.
     * @param address
     */
    public static String extractBase64Dest(String address) {
        if (address == null)
            return null;

        // remove spaces and newlines
        // doesn't affect extraction from "name <dest>" addresses
        address.replaceAll("[\\s\\r\\n]+", "");
        // remove possible prefixes
        if (address.startsWith("mailto:") ||
                address.startsWith("i2pbote:") ||
                address.startsWith("bote:"))
            address = address.substring(address.indexOf(':') + 1);

        // find the crypto implementation for this key length
        for (CryptoImplementation cryptoImpl: CryptoFactory.getInstances()) {
            int base64Length = cryptoImpl.getBase64PublicKeyPairLength();   // length of an email destination with this CryptoImplementation
            
            if (address.length() == base64Length)
                return address;
            
            // Check if the string contains base64Length chars in angle brackets
            int ltIndex = address.indexOf('<');
            int gtIndex = address.indexOf('>', ltIndex);
            if (ltIndex>=0 && ltIndex+base64Length+1==gtIndex)
                return address.substring(ltIndex+1, gtIndex);
            
            // Check if the string is of the form EmailDest@foo
            if (address.indexOf('@') == base64Length)
                return address.substring(0, base64Length+1);
        }
        
        return null;
    }
    
    public CryptoImplementation getCryptoImpl() {
        return cryptoImpl;
    }
    
    public PublicKey getPublicEncryptionKey() {
        return publicEncryptionKey;
    }

    public PublicKey getPublicSigningKey() {
        return publicSigningKey;
    }
    
    public byte[] toByteArray() {
        PublicKeyPair keys = new PublicKeyPair(publicEncryptionKey, publicSigningKey);
        return cryptoImpl.toByteArray(keys);
    }
    
    public Hash getHash() {
        // TODO cache the hash value?
        return SHA256Generator.getInstance().calculateHash(toByteArray());
    }
    
    /**
     * Returns the two public keys in Base64 representation.
     */
    public String getKey() {
        PublicKeyPair keys = new PublicKeyPair(publicEncryptionKey, publicSigningKey);
        try {
            return cryptoImpl.toBase64(keys);
        } catch (GeneralSecurityException e) {
            log.error("Can't get email destination keys.", e);
            return "<Error>: " + e.getLocalizedMessage();
        }
    }
    
    public String toBase64() {
        return getKey();
    }
    
    @Override
    public String toString() {
        return getKey();
    }
}