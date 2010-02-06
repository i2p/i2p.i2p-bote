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

import i2p.bote.email.Email;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.mail.MessagingException;

import net.i2p.util.Log;

/**
 * Stores emails in a directory on the filesystem. For each email, two files are created; the actual
 * email and a status file.
 * Status files and email files have the same name, except for the extension.
 * Even emails that need to be fragmented are stored as a whole.
 * Message IDs are used for filenames.
 * 
 * Status files contain a status for each recipient address.
 */
public class Outbox extends EmailFolder {
	private static final String STATUS_FILE_EXTENSION = ".status";
//	private static final String PARAM_QUEUE_DATE = "X-QueueDate";
	private static final Log log = new Log(Outbox.class);
	
	public Outbox(File storageDir) {
		super(storageDir);
	}
	
	// store one email file + one status file.
	@Override
	public void add(Email email) throws IOException, MessagingException {
        // write out the email file
	    super.add(email);
		
		// collect info for status file
		String queueDate = String.valueOf(System.currentTimeMillis());
		
		// write out the status file
		File statusFile = getStatusFile(email);
		FileWriter statusFileWriter = new FileWriter(statusFile);
		statusFileWriter.write(queueDate);
		statusFileWriter.close();
	}
	
	private File getStatusFile(Email email) {
		return new File(storageDir, email.getMessageID() + STATUS_FILE_EXTENSION);
	}

	// delete an email file + the status file
    @Override
    public void delete(Email email) {
	    super.delete(email);
	    
        if (!getStatusFile(email).delete())
            log.error("Cannot delete file: '" + getStatusFile(email) + "'");
    }

	/**
	 * 
	 * @param email
	 * @param relayInfo A 0-length array means no relays were used, i.e. the email was sent directly to the recipient.
	 * @param statusMessage
	 */
	public void updateStatus(Email email, int[] relayInfo, String statusMessage) {
		// TODO write out a new status file. filename is the msg id, statusMessage goes into the file.
	}
}