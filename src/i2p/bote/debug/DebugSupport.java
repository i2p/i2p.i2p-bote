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

package i2p.bote.debug;

import i2p.bote.Configuration;
import i2p.bote.Util;
import i2p.bote.fileencryption.EncryptedInputStream;
import i2p.bote.fileencryption.FileEncryptionUtil;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.fileencryption.PasswordHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.i2p.util.Log;

public class DebugSupport {
    private Log log = new Log(DebugSupport.class);
    private Configuration configuration;
    private PasswordHolder passwordHolder;

    public DebugSupport(Configuration configuration, PasswordHolder passwordHolder) {
        this.configuration = configuration;
        this.passwordHolder = passwordHolder;
    }
    
    /**
     * Tests all encrypted I2P-Bote files and returns a list containing those that
     * cannot be decrypted.
     * @return A list of problem files, or an empty list if no problems were found
     * @throws PasswordException
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public List<File> getUndecryptableFiles() throws PasswordException, IOException, GeneralSecurityException {
        // make sure the password is correct
        byte[] password = passwordHolder.getPassword();
        if (password == null)
            throw new PasswordException();
        File passwordFile = configuration.getPasswordFile();
        boolean correct = FileEncryptionUtil.isPasswordCorrect(password, passwordFile);
        if (!correct)
            throw new PasswordException();
        
        // make a list of all encrypted files
        List<File> files = new ArrayList<File>();
        files.add(configuration.getIdentitiesFile());
        files.add(configuration.getAddressBookFile());
        File[] emailFolders = new File[] {configuration.getInboxDir(), configuration.getOutboxDir(), configuration.getSentFolderDir(), configuration.getTrashFolderDir()};;
        for (File dir: emailFolders)
            files.addAll(Arrays.asList(dir.listFiles()));
        
        for (Iterator<File> iter=files.iterator(); iter.hasNext(); ) {
            File file = iter.next();
            FileInputStream stream = new FileInputStream(file);
            try {
                Util.readBytes(new EncryptedInputStream(stream, password));
                // no PasswordException or other exception occurred, so the file is good
                iter.remove();
            } catch (Exception e) {
                // leave the file in the list and log
                log.debug("Can't decrypt file <" + file.getAbsolutePath() + ">", e);
            } finally {
                if (stream != null)
                    stream.close();
            }
        }
        
        return files;
    }
}