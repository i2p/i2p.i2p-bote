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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import net.i2p.client.I2PSessionException;
import net.i2p.util.Log;

/**
 * Represents a set of <code>EmailIdentities</code>.
 */
public class Identities implements Iterable<EmailIdentity> {
    private Log log = new Log(Identities.class);
    private File identitiesFile;
    private List<EmailIdentity> identities;

    /**
     * Constructs <code>Identities</code> from a text file. Each identity is defined
     * by one line that contains two to four tab-separated fields:
     * Email Destination key, Public Name, Description, and Email Address.
     * The first two are mandatory, the last two are optional.
     * 
     * Additionally, the file can set a default Email Destination by including a
     * line that starts with "Default ", followed by an Email Destination key.
     * The destination key must match one of the Email Destinations defined in
     * the file.
     * @param identitiesFile
     */
    public Identities(File identitiesFile) {
        this.identitiesFile = identitiesFile;
        identities = Collections.synchronizedList(new ArrayList<EmailIdentity>());
        String defaultIdentityString = null;
        
        if (!identitiesFile.exists()) {
            log.debug("Identities file does not exist: <" + identitiesFile.getAbsolutePath() + ">");
            return;
        }
        
        log.debug("Reading identities file: <" + identitiesFile.getAbsolutePath() + ">");
        BufferedReader input = null;
        try {
            input = new BufferedReader(new FileReader(identitiesFile));
            
            while (true) {
                String line = input.readLine();
                if (line == null)   // EOF
                    break;
                
                if (line.toLowerCase().startsWith("default"))
                    defaultIdentityString = line.substring("default ".length());
                else {
                    EmailIdentity identity = parse(line);
                    if (identity != null)
                        identities.add(identity);
                }
            }
            
            // set the default identity; if none defined, make the first one the default
            EmailIdentity defaultIdentity = get(defaultIdentityString);
            if (defaultIdentity != null)
                defaultIdentity.setDefault(true);
            else if (!identities.isEmpty())
                identities.get(0).setDefault(true);
        } catch (IOException e) {
            log.error("Can't read identities file.", e);
        }
        finally {
            if (input != null)
                try {
                    input.close();
                }
                catch (IOException e) {
                    log.error("Error closing input stream.", e);
                }
        }
    }
 
    private EmailIdentity parse(String emailIdentityString) {
        String[] fields = emailIdentityString.split("\\t", 4);
        if (fields.length < 2) {
            log.debug("Unparseable email identity: <" + emailIdentityString + ">");
            return null;
        }
        try {
            EmailIdentity identity = new EmailIdentity(fields[0]);
            if (fields.length > 1)
                identity.setPublicName(fields[1]);
            if (fields.length > 2)
                identity.setDescription(fields[2]);
            if (fields.length > 3)
                identity.setEmailAddress(fields[3]);
            return identity;
        }
        catch (PatternSyntaxException e) {
            log.debug("Unparseable email identity: <" + emailIdentityString + ">");
            return null;
        } catch (I2PSessionException e) {
            log.debug("Invalid email identity: <" + fields[0] + ">");
            return null;
        }
    }
    
    /**
     * This is the counterpart of the <code>parse</code> method. It encodes a {@link EmailIdentity} into
     * an entry for the identities file.
     * @param identity
     * @return
     */
    private String toFileFormat(EmailIdentity identity) {
        StringBuilder string = new StringBuilder();
        string = string.append(identity.getFullKey());
        string = string.append("\t");
        string = string.append(identity.getPublicName());
        string = string.append("\t");
        if (identity.getDescription() != null)
            string = string.append(identity.getDescription());
        string = string.append("\t");
        if (identity.getEmailAddress() != null)
            string = string.append(identity.getEmailAddress());
        return string.toString();
    }
    
    public void save() throws IOException {
        String newLine = System.getProperty("line.separator");
        Writer writer = new BufferedWriter(new FileWriter(identitiesFile));
        try {
            EmailIdentity defaultIdentity = getDefault();
            if (defaultIdentity != null)
                writer.write("Default " + defaultIdentity.getKey() + newLine);
            
            for (EmailIdentity identity: identities)
                writer.write(toFileFormat(identity) + newLine);
        }
        catch (IOException e) {
            log.error("Can't save email identities to file <" + identitiesFile.getAbsolutePath() + ">.", e);
            throw e;
        }
        finally {
            writer.close();
        }
    }
    
    public void add(EmailIdentity identity) {
        if (identities.isEmpty())
            identity.setDefault(true);
        identities.add(identity);
    }
    
    public void remove(String key) {
        EmailIdentity identity = get(key);
        if (identity != null) {
            identities.remove(identity);
            
            // if we deleted the default identity, set a new default
            if (identity.isDefault() && !identities.isEmpty())
                identities.get(0).setDefault(true);
        }
    }
    
    /**
     * Sets the default identity. Assumes this <code>Identities</code> already
     * contains <code>defaultIdentity</code>.
     * @return
     */
    public void setDefault(EmailIdentity defaultIdentity) {
        // clear the old default
        for (EmailIdentity identity: identities)
            identity.setDefault(false);
        
        defaultIdentity.setDefault(true);
    }
    
    /**
     * Returns the default identity, or <code>null</code> if no default is set.
     * @return
     */
    public EmailIdentity getDefault() {
        for (EmailIdentity identity: identities)
            if (identity.isDefault())
                return identity;
        
        return null;
    }
    
    public EmailIdentity get(int i) {
        return identities.get(i);
    }

    /**
     * Looks up an {@link EmailIdentity} that has the same public encryption key and the
     * same public signing key as a given {@link EmailDestination}.
     * Returns <code>null</code> if nothing is found.
     * @param key
     * @return
     */
    public EmailIdentity get(EmailDestination destination) {
        for (EmailIdentity identity: identities)
            if (identity.getPublicEncryptionKey().equals(destination.getPublicEncryptionKey())
                    && identity.getPublicSigningKey().equals(destination.getPublicSigningKey()))
                return identity;
        return null;
    }
    
    /**
     * Looks up an {@link EmailIdentity} by its Base64 key (the two public keys, to be
     * more precise).
     * Returns <code>null</code> if nothing is found.
     * @param key
     * @return
     */
    public EmailIdentity get(String key) {
        for (EmailIdentity identity: identities)
            if (identity.getKey().equals(key))
                return identity;
        return null;
    }
    
    public Collection<EmailIdentity> getAll() {
        return identities;
    }
    
    public int size() {
        return identities.size();
    }
    
    @Override
    public Iterator<EmailIdentity> iterator() {
        return identities.iterator();
    }
}