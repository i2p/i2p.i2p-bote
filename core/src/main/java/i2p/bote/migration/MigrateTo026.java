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

package i2p.bote.migration;

import i2p.bote.Configuration;
import i2p.bote.Util;
import i2p.bote.fileencryption.EncryptedOutputStream;
import i2p.bote.fileencryption.FileEncryptionConstants;
import i2p.bote.fileencryption.PasswordCache;
import i2p.bote.fileencryption.PasswordException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import net.i2p.util.Log;

/**
 * Migrates the address book, email identities, and all emails to the encrypted file format.<br/>
 */
class MigrateTo026 {
    private Log log = new Log(MigrateTo026.class);
    private PasswordCache passwordCache;
    
    /**
     * This method won't corrupt any data if the data has already been migrated to the latest version,
     * because only unencrypted files are converted.
     * @param configuration
     * @throws Exception
     */
    void migrateIfNeeded(Configuration configuration) throws Exception {
        log.debug("Migrating any pre-0.2.6 files...");
        
        passwordCache = new PasswordCache(configuration);
        // encrypt with the default password
        passwordCache.setPassword(new byte[0]);

        // convert the identities file
        File oldIdentitiesFile = new File(configuration.getIdentitiesFile().getParentFile(), "identities.txt");
        if (oldIdentitiesFile.exists()) {
            log.debug("Migrating identities file: <" + oldIdentitiesFile + ">");
            File newIdentitiesFile = configuration.getIdentitiesFile();
            encrypt(oldIdentitiesFile, newIdentitiesFile);
            oldIdentitiesFile.delete();
        }
        
        // convert the address book
        File oldAddrBookFile = new File(configuration.getAddressBookFile().getParentFile(), "addressBook.txt");
        if (oldAddrBookFile.exists()) {
            log.debug("Migrating address book: <" + oldIdentitiesFile + ">");
            File newAddrBookFile = configuration.getAddressBookFile();
            encrypt(oldAddrBookFile, newAddrBookFile);
            oldAddrBookFile.delete();
        }
        
        // convert email folders
        migrateEmailsIfNeeded(configuration.getInboxDir());
        migrateEmailsIfNeeded(configuration.getOutboxDir());
        migrateEmailsIfNeeded(configuration.getSentFolderDir());
        migrateEmailsIfNeeded(configuration.getTrashFolderDir());
    }
    
    private void migrateEmailsIfNeeded(File directory) throws IOException, PasswordException, GeneralSecurityException {
        if (!directory.exists())
            return;
        
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".mail");
            }
        };
        
        for (File file: directory.listFiles(filter))
            if (!isEncrypted(file)) {
                log.debug("Migrating email file: <" + file + ">");
                encrypt(file, file);
            }
    }
    
    private boolean isEncrypted(File file) throws IOException {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            byte[] firstFour = new byte[4];
            inputStream.read(firstFour);
            return Arrays.equals(firstFour, FileEncryptionConstants.START_OF_FILE);
        }
        finally {
            if (inputStream != null)
                inputStream.close();
        }
    }
    
    private void encrypt(File oldFile, File newFile) throws IOException, PasswordException, GeneralSecurityException {
        InputStream inputStream = null;
        byte[] contents = null;
        try {
            inputStream = new FileInputStream(oldFile);
            contents = Util.readBytes(inputStream);
        }
        finally {
            if (inputStream != null)
                inputStream.close();
        }
        
        EncryptedOutputStream encryptedStream = null;
        try {
            OutputStream fileOutputStream = new FileOutputStream(newFile);
            encryptedStream = new EncryptedOutputStream(fileOutputStream, passwordCache);
            if (contents != null)
                encryptedStream.write(contents);
        }
        finally {
            if (encryptedStream != null)
                encryptedStream.close();
        }
    }
}