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

package i2p.bote.folder;

import i2p.bote.packet.Email;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import net.i2p.util.Log;

/**
 * Stores emails in a directory on the file system. Each email is stored in one file.
 * The filename is the message Id plus an extension.
 */
public class EmailFolder extends Folder<Email> {
    protected static final String EMAIL_FILE_EXTENSION = ".mail";
    
    private Log log = new Log(EmailFolder.class);
    
    public EmailFolder(File storageDir) {
        super(storageDir, EMAIL_FILE_EXTENSION);
    }
    
    // store an email file
    public void add(Email email) throws IOException {
        // write out the email file
        File emailFile = getEmailFile(email);
        log.info("Storing email in outbox: '" + emailFile.getAbsolutePath() + "'");
        OutputStream emailOutputStream = new FileOutputStream(emailFile);
        email.writeTo(emailOutputStream);
        emailOutputStream.close();
    }
    
    private File getEmailFile(Email email) {
        return new File(storageDir, email.getMessageID() + EMAIL_FILE_EXTENSION);
    }

    public void delete(Email email) {
        if (!getEmailFile(email).delete())
            log.error("Cannot delete file: '" + getEmailFile(email) + "'");
    }

    @Override
    protected Email createFolderElement(File file) throws Exception {
        FileInputStream inputStream = new FileInputStream(file);
        return new Email(inputStream);
    }
}