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
import i2p.bote.crypto.PrivateKeyPair;
import i2p.bote.crypto.PublicKeyPair;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PrivateKey;

public class EmailIdentity extends EmailDestination {
    private PrivateKey privateEncryptionKey;
    private PrivateKey privateSigningKey;
    private String publicName;
    private String description;   // optional
    private String emailAddress;   // optional
    private boolean isDefault;

    /**
     * Creates a random <code>EmailIdentity</code>.
     * @param cryptoImpl
     * @throws GeneralSecurityException 
     */
    public EmailIdentity(CryptoImplementation cryptoImpl) throws GeneralSecurityException {
        super();
        this.cryptoImpl = cryptoImpl;
        KeyPair encryptionKeys = cryptoImpl.generateEncryptionKeyPair();
        KeyPair signingKeys = cryptoImpl.generateSigningKeyPair();
        
        publicEncryptionKey = encryptionKeys.getPublic();
        privateEncryptionKey = encryptionKeys.getPrivate();
        publicSigningKey = signingKeys.getPublic();
        privateSigningKey = signingKeys.getPrivate();
    }

    /**
     * Creates a <code>EmailIdentity</code> from a Base64-encoded string.
     * The format can be any format supported by one of the {@link CryptoImplementation}s;
     * the length of the string must match {@link CryptoImplementation#getBase64CompleteKeySetLength()}.
     * @param base64Key
     * @throws GeneralSecurityException 
     */
    public EmailIdentity(String base64Key) throws GeneralSecurityException {
        // find the crypto implementation for this key length
        for (CryptoImplementation cryptoImpl: CryptoFactory.getInstances()) {
            int base64Length = cryptoImpl.getBase64CompleteKeySetLength();   // length of an email identity with this CryptoImplementation
            if (base64Key.length() == base64Length) {
                this.cryptoImpl = cryptoImpl;
                break;
            }
        }
        if (cryptoImpl == null)
            throw new InvalidKeyException("Not a valid Email Identity, no CryptoImplementation matches length " + base64Key.length() + ": <" + base64Key + ">");
            
        PublicKeyPair publicKeys = cryptoImpl.createPublicKeyPair(base64Key);
        String base64PrivateKeys = base64Key.substring(cryptoImpl.getBase64PublicKeyPairLength());   // the two private keys start after the two public keys
        PrivateKeyPair privateKeys = cryptoImpl.createPrivateKeyPair(base64PrivateKeys);
        
        publicEncryptionKey = publicKeys.encryptionKey;
        privateEncryptionKey = privateKeys.encryptionKey;
        publicSigningKey = publicKeys.signingKey;
        privateSigningKey = privateKeys.signingKey;
    }
    
    public PrivateKey getPrivateEncryptionKey() {
        return privateEncryptionKey;
    }
    
    public PrivateKey getPrivateSigningKey() {
        return privateSigningKey;
    }

    public void setPublicName(String publicName) {
        this.publicName = publicName;
    }

    public String getPublicName() {
        return publicName;
    }

    public void setDescription(String name) {
        this.description = name;
    }

    public String getDescription() {
        return description;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Returns the two key pairs (public + private) as one Base64-encoded string.
     * @throws GeneralSecurityException 
     */
    public String getFullKey() throws GeneralSecurityException {
        PublicKeyPair publicKeys = new PublicKeyPair(publicEncryptionKey, publicSigningKey);
        PrivateKeyPair privateKeys = new PrivateKeyPair(privateEncryptionKey, privateSigningKey);
        
        String pubKeys = cryptoImpl.toBase64(publicKeys);
        String privKeys = cryptoImpl.toBase64(privateKeys);
        return pubKeys + privKeys;
    }
    
    @Override
    public String toString() {
        return getKey() + " address=<" + getEmailAddress() + "> identity name=<" + getDescription() + "> visible name=<" + getPublicName() + ">";
    }
}