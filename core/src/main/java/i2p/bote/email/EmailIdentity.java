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

import com.lambdaworks.codec.Base64;

import net.i2p.crypto.SHA256Generator;
import net.i2p.data.Hash;
import net.i2p.util.Log;

import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Properties;

import i2p.bote.Configuration;
import i2p.bote.Util;
import i2p.bote.crypto.CryptoFactory;
import i2p.bote.crypto.CryptoImplementation;
import i2p.bote.crypto.PrivateKeyPair;
import i2p.bote.crypto.PublicKeyPair;
import i2p.bote.util.SortedProperties;

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
    private IdentityConfigImpl identityConfig;

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
            throw new IllegalDestinationParametersException(vanityPrefix.charAt(0), cryptoImpl.getBase64InitialCharacters());
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

        identityConfig = new IdentityConfigImpl();
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

        identityConfig = new IdentityConfigImpl();
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

    public void loadConfig(Properties properties, String prefix, boolean overwrite) {
        identityConfig.loadFromProperties(properties, prefix, overwrite);
    }

    public Properties saveConfig(String prefix) {
        return identityConfig.saveToProperties(prefix);
    }

    public IdentityConfig getConfig() {
        return identityConfig;
    }

    public IdentityConfig getWrappedConfig(Configuration configuration) {
        return identityConfig.wrap(configuration);
    }

    public void setIncludeInGlobalCheck(boolean include) {
        identityConfig.setIncludeInGlobalCheck(include);
    }

    public boolean getIncludeInGlobalCheck() {
        return identityConfig.getIncludeInGlobalCheck();
    }

    public interface IdentityConfig {
        public int getRelayRedundancy();
        public void setRelayMinDelay(int minDelay);
        public int getRelayMinDelay();
        public void setRelayMaxDelay(int maxDelay);
        public int getRelayMaxDelay();
        public void setNumStoreHops(int numHops);
        public int getNumStoreHops();
    }

    private class IdentityConfigImpl implements IdentityConfig {
        private static final String PARAMETER_INCLUDE_IN_GLOBAL_CHECK_MAIL = "includeInGlobalCheck";

        private static final boolean DEFAULT_INCLUDE_IN_GLOBAL_CHECK_MAIL = true;

        // Overrides for default configuration
        private static final String PARAMETER_RELAY_REDUNDANCY = "relayRedundancy";
        private static final String PARAMETER_RELAY_MIN_DELAY = "relayMinDelay";
        private static final String PARAMETER_RELAY_MAX_DELAY = "relayMaxDelay";
        private static final String PARAMETER_NUM_STORE_HOPS = "numSendHops";

        private Log log = new Log(IdentityConfig.class);
        private Properties properties;
        private Configuration configuration;

        public IdentityConfigImpl() {
            properties = new Properties();
        }

        private IdentityConfigImpl(Properties properties, Configuration configuration) {
            this.properties = properties;
            this.configuration = configuration;
        }

        public void loadFromProperties(Properties sourceProperties, String prefix, boolean overwrite) {
            if (prefix == null)
                prefix = "";
            if (overwrite)
                properties = new Properties();

            for (String key : sourceProperties.stringPropertyNames()) {
                if (key.startsWith(prefix) && sourceProperties.getProperty(key) != null)
                    properties.setProperty(key.substring(prefix.length()), sourceProperties.getProperty(key));
            }
        }

        public Properties saveToProperties(String prefix) {
            SortedProperties prefixedProperties = new SortedProperties();

            for (String key : properties.stringPropertyNames()) {
                if (properties.getProperty(key) != null)
                    prefixedProperties.setProperty(prefix + key, properties.getProperty(key));
            }

            return prefixedProperties;
        }

        public IdentityConfig wrap(Configuration configuration) {
            return new IdentityConfigImpl(properties, configuration);
        }

        // Identity-only configuration

        public void setIncludeInGlobalCheck(boolean include) {
            properties.setProperty(PARAMETER_INCLUDE_IN_GLOBAL_CHECK_MAIL, String.valueOf(include));
        }

        /**
         * Controls whether the identity should be checked for email when using
         * the global "Check mail" button.
         */
        public boolean getIncludeInGlobalCheck() {
            return getBooleanParameter(PARAMETER_INCLUDE_IN_GLOBAL_CHECK_MAIL, DEFAULT_INCLUDE_IN_GLOBAL_CHECK_MAIL);
        }

        // Per-identity overrides of default configuration

        /**
         * Returns the number of relay chains that should be used per Relay Request.
         */
        public int getRelayRedundancy() {
            return getIntParameter(PARAMETER_RELAY_REDUNDANCY,
                    configuration == null ? -1 : configuration.getRelayRedundancy());
        }

        public void setRelayMinDelay(int minDelay) {
            if (minDelay < 0)
                properties.remove(PARAMETER_RELAY_MIN_DELAY);
            else
                properties.setProperty(PARAMETER_RELAY_MIN_DELAY, String.valueOf(minDelay));
        }

        /**
         * Returns the minimum amount of time in minutes that a Relay Request is delayed.
         */
        public int getRelayMinDelay() {
            return getIntParameter(PARAMETER_RELAY_MIN_DELAY,
                    configuration == null ? -1 : configuration.getRelayMinDelay());
        }

        public void setRelayMaxDelay(int maxDelay) {
            if (maxDelay < 0)
                properties.remove(PARAMETER_RELAY_MAX_DELAY);
            else
                properties.setProperty(PARAMETER_RELAY_MAX_DELAY, String.valueOf(maxDelay));
        }

        /**
         * Returns the maximum amount of time in minutes that a Relay Request is delayed.
         */
        public int getRelayMaxDelay() {
            return getIntParameter(PARAMETER_RELAY_MAX_DELAY,
                    configuration == null ? -1 : configuration.getRelayMaxDelay());
        }

        public void setNumStoreHops(int numHops) {
            if (numHops < 0)
                properties.remove(PARAMETER_NUM_STORE_HOPS);
            else
                properties.setProperty(PARAMETER_NUM_STORE_HOPS, String.valueOf(numHops));
        }

        /**
         * Returns the number of relays that should be used when sending a DHT store request.
         * @return -1 if unset, or a non-negative number otherwise.
         */
        public int getNumStoreHops() {
            return getIntParameter(PARAMETER_NUM_STORE_HOPS,
                    configuration == null ? -1 : configuration.getNumStoreHops());
        }

        private boolean getBooleanParameter(String parameterName, boolean defaultValue) {
            try {
                return Util.getBooleanParameter(properties, parameterName, defaultValue);
            } catch (IllegalArgumentException e) {
                log.warn("getBooleanParameter failed, using default", e);
                return defaultValue;
            }
        }

        private int getIntParameter(String parameterName, int defaultValue) {
            try {
                return Util.getIntParameter(properties, parameterName, defaultValue);
            } catch (NumberFormatException e) {
                log.warn("getIntParameter failed, using default", e);
                return defaultValue;
            }
        }
    }
}
