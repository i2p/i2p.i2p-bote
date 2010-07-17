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

import static i2p.bote.Util._;
import i2p.bote.email.AddressDisplayFilter;
import i2p.bote.email.Email;
import i2p.bote.email.EmailAttribute;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An {@link EmailFolder} that maintains a status for each email.<br/>
 * The status is not written to a file. It is reset when the application
 * is restarted.<br/>
 * Statuses can be any string. Their purpose is to inform the user
 * about the sending progress.
 */
public class Outbox extends EmailFolder {
    private static final String DEFAULT_STATUS = "Queued";
	
    private Map<String, String> statusMap;   // maps message IDs to status strings
    
	public Outbox(File storageDir) {
		super(storageDir);
		statusMap = new ConcurrentHashMap<String, String>();
	}

    /**
     * Overridden to handle the <code>STATUS</code> attribute.
     */
    @Override
    public List<Email> getElements(AddressDisplayFilter displayFilter, EmailAttribute sortColumn, boolean descending) {
        if (!EmailAttribute.STATUS.equals(sortColumn))
            return super.getElements(displayFilter, sortColumn, descending);
            
        // sort by status
        List<Email> emails = getElements();
        Comparator<Email> comparator = new Comparator<Email>() {
            @Override
            public int compare(Email email1, Email email2) {
                return getStatus(email1).compareTo(getStatus(email2));
            }
        };
        if (descending)
            comparator = Collections.reverseOrder(comparator);
        Collections.sort(emails, comparator);
        return emails;
    }

    public void setStatus(Email email, String status) {
        String messageId = email.getMessageID();
        if (messageId != null)
            statusMap.put(messageId, status);
    }

    /**
     * Returns the status of an email with a given message ID.
     * If no email exists under the message ID, or if no status is set,
     * <code>DEFAULT_STATUS</code> is returned.
     * @param messageId The message ID of the email
     * @return
     */
    private String getStatus(String messageId) {
        if (statusMap.containsKey(messageId))
            return statusMap.get(messageId);
        else
            return _(DEFAULT_STATUS);
    }
    
    /**
     * Returns the status of an {@link Email}.
     * If the email doesn't exist in the outbox, or if no status is set,
     * <code>DEFAULT_STATUS</code> is returned.
     * @param email
     */
    public String getStatus(Email email) {
        String messageId = email.getMessageID();
        if (messageId == null)
            return _(DEFAULT_STATUS);
        else
            return getStatus(messageId);
    }
    
    public boolean isStatusSet(Email email) {
        if (email == null)
            return false;
        else
            return statusMap.containsKey(email.getMessageID());
    }
}
