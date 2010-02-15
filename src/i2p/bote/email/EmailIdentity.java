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

import net.i2p.client.I2PClient;
import net.i2p.client.I2PClientFactory;
import net.i2p.client.I2PSession;
import net.i2p.client.I2PSessionException;
import net.i2p.data.Base64;
import net.i2p.data.PrivateKey;
import net.i2p.data.SigningPrivateKey;
import net.i2p.util.Log;

public class EmailIdentity extends EmailDestination {
    private Log log = new Log(EmailIdentity.class);
    private PrivateKey privateEncryptionKey;
    private SigningPrivateKey privateSigningKey;
    private String publicName;
    private String description;   // optional
    private String emailAddress;   // optional
    private boolean isDefault;

    /**
     * Creates a random <code>EmailIdentity</code>.
     */
    public EmailIdentity() {
        // key initialization happens in the super constructor, which calls initKeys
    }

    /**
     * Creates a <code>EmailIdentity</code> from a Base64-encoded string. The format is the same as
     * for Base64-encoded local I2P destinations, except there is no null certificate.
     * @param key
     * @throws I2PSessionException
     */
    public EmailIdentity(String key) throws I2PSessionException {
        key = key.substring(0, 512) + "AAAA" + key.substring(512);   // insert a null certificate for I2PClient.createSession()
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.decode(key));
        
        I2PClient i2pClient = I2PClientFactory.createClient();
        I2PSession i2pSession = i2pClient.createSession(inputStream, null);
        initKeys(i2pSession);
    }
    
    public PrivateKey getPrivateEncryptionKey() {
        return privateEncryptionKey;
    }
    
    public SigningPrivateKey getPrivateSigningKey() {
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

    protected void initKeys(I2PSession i2pSession) {
        super.initKeys(i2pSession);
        privateEncryptionKey = i2pSession.getDecryptionKey();
        privateSigningKey = i2pSession.getPrivateKey();
    }
    
    private byte[] getKeysAsArray() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try {
            byteStream.write(getPublicEncryptionKey().getData());
            byteStream.write(getPublicSigningKey().getData());
            byteStream.write(getPrivateEncryptionKey().getData());
            byteStream.write(getPrivateSigningKey().getData());
        }
        catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
        }
        return byteStream.toByteArray();
    }
    
    /**
     * Returns the two key pairs (public + private) as one Base64-encoded string.
     * @return
     */
    public String getFullKey() {
        return Base64.encode(getKeysAsArray());
    }
    
    @Override
    public String toString() {
        return getKey() + " address=<" + getEmailAddress() + "> identity name=<" + getDescription() + "> visible name=<" + getPublicName() + ">";
    }
}