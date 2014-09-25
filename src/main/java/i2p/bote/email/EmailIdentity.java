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
import i2p.bote.Util;
import i2p.bote.crypto.CryptoFactory;
import i2p.bote.crypto.CryptoImplementation;
import i2p.bote.crypto.PrivateKeyPair;
import i2p.bote.crypto.PublicKeyPair;

import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PrivateKey;

import net.i2p.crypto.SHA256Generator;
import net.i2p.data.Hash;

import com.lambdaworks.codec.Base64;

public class EmailIdentity extends EmailDestination {
    private final static Charset UTF8 = Charset.forName("UTF-8");
    
    private PrivateKey privateEncryptionKey;
    private PrivateKey privateSigningKey;
    private String publicName;
    private String description;   // optional
    private String emailAddress;   // optional
    private byte[] picture;
    private String text;
    private Fingerprint fingerprint;
    private boolean published;
    private boolean defaultIdentity;

    /**
     * Creates a random <code>EmailIdentity</code>.
     * @param cryptoImpl
     * @param vanityPrefix Base64 chars that the Email Destination should start with; <code>null</code> or an empty string for no vanity Destination.
     * @throws GeneralSecurityException 
     * @throws IllegalDestinationParametersException if <code>cryptoImpl</code> and <code>vanityPrefix</code> aren't compatible
     */
    public EmailIdentity(CryptoImplementation cryptoImpl, String vanityPrefix) throws GeneralSecurityException, IllegalDestinationParametersException {
        super();
        this.cryptoImpl = cryptoImpl;
        
        if ("".equals(vanityPrefix))
            vanityPrefix = null;
        if (vanityPrefix!=null && !cryptoImpl.getBase64InitialCharacters().contains(vanityPrefix.substring(0, 1))) {
            String errorMsg = "This encryption type does not support destinations that start with a \"{0}\". Valid initial characters are {1}.";
            throw new IllegalDestinationParametersException(_(errorMsg, vanityPrefix.charAt(0), cryptoImpl.getBase64InitialCharacters()));
        }
        
        KeyPair encryptionKeys;
        do {
            encryptionKeys = cryptoImpl.generateEncryptionKeyPair();
        } while (vanityPrefix!=null && !cryptoImpl.encryptionKeyToBase64(encryptionKeys.getPublic()).startsWith(vanityPrefix));
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
            throw new InvalidKeyException("Not a valid Email Identity, no CryptoImplementation (out of " + CryptoFactory.getInstances().size() + ") matches length " + base64Key.length() + ": <" + base64Key + ">");

        String base64PublicKeys = base64Key.substring(0, cryptoImpl.getBase64PublicKeyPairLength()); // the two private keys start after the two public keys
        PublicKeyPair publicKeys = cryptoImpl.createPublicKeyPair(base64PublicKeys);
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

    public void setDescription(String description) {
        this.description = description;
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

    public void setPublished(boolean published) {
        this.published = published;
    }
    
    public boolean isPublished() {
        return published;
    }

    public void setPicture(byte[] picture) {
        this.picture = picture;
    }
    
    public byte[] getPicture() {
        return picture;
    }

    public void setPictureBase64(String pictureBase64) {
        if (pictureBase64 == null)
            picture = null;
        else
            picture = Base64.decode(pictureBase64.toCharArray());
    }

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
    
    public String getText() {
        return text;
    }

    public void generateFingerprint() throws GeneralSecurityException {
        fingerprint = Fingerprint.generate(this);
    }

    public void setFingerprint(Fingerprint fingerprint) {
        this.fingerprint = fingerprint;
    }
    
    public Fingerprint getFingerprint() throws GeneralSecurityException {
        return fingerprint;
    }
    
    public static Hash calculateHash(String name) {
        name = name.trim();
        if (name.endsWith("@bote.i2p"))
            name = name.substring(0, name.length()-"@bote.i2p".length());
        byte[] nameBytes = name.toLowerCase().getBytes(UTF8);
        return SHA256Generator.getInstance().calculateHash(nameBytes);
    }
    
    public void setDefaultIdentity(boolean defaultIdentity) {
        this.defaultIdentity = defaultIdentity;
    }

    public boolean isDefaultIdentity() {
        return defaultIdentity;
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