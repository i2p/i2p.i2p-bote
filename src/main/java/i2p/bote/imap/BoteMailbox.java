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

package i2p.bote.imap;

import static i2p.bote.Util._;
import i2p.bote.email.Email;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.FolderListener;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.mail.MessagingException;
import javax.mail.Flags.Flag;

import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;

/**
 * Implementation of {@link org.apache.james.mailbox.store.mail.model.Mailbox}.
 */
public class BoteMailbox extends SimpleMailbox<String> {
    private EmailFolder folder;
    private SortedMap<Email, BoteMessage> messageMap;
    private List<BoteMessage> messages;
    private Long uid;
    private long modSeq;
    private FolderListener folderListener;

    public BoteMailbox(EmailFolder folder, long uidValidity, long uid) {
        super(new MailboxPath("I2P-Bote", "bote", folder.getName()), uidValidity);
        this.folder = folder;
        this.messageMap = Collections.synchronizedSortedMap(new TreeMap<Email, BoteMessage>(new Comparator<Email>() {
            @Override
            public int compare(Email email1, Email email2) {
                // Try received dates first, this is set for all received.
                // emails. If not set, this is a sent email, use sent date.
                Date msg1date = email1.getReceivedDate();
                if (msg1date == null)
                    try {
                        msg1date = email1.getSentDate();
                    } catch (MessagingException e) {}

                Date msg2date = email2.getReceivedDate();
                if (msg2date == null)
                    try {
                        msg2date = email2.getSentDate();
                    } catch (MessagingException e) {}

                if (msg1date != null && msg2date != null)
                    return msg1date.compareTo(msg2date);

                // Catch-all
                return email1.getMessageID().compareTo(email2.getMessageID());
            }
        }));
        this.uid = uid;
        
        modSeq = System.currentTimeMillis();
        modSeq <<= 32;

        startListening();
        try {
            List<Email> emails = folder.getElements();
            for (Email email : emails)
                messageMap.put(email, new BoteMessage(email, getFolderName()));
            updateMessages();
        } catch (PasswordException e) {
            throw new RuntimeException(_("Password required or invalid password provided"), e);
        }
    }

    protected void startListening() {
        folderListener = new FolderListener() {
            public void elementAdded(String messageId) {
                try {
                    // Add new emails to map
                    Email email = folder.getEmail(messageId);
                    email.setFlag(Flag.RECENT, true);
                    messageMap.put(email, new BoteMessage(email, getFolderName()));
                    updateMessages();
                } catch (PasswordException e) {
                    throw new RuntimeException(_("Password required or invalid password provided"), e);
                } catch (MessagingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            public void elementUpdated() {
                // Noop, BoteMessage has a reference to the Email
            }

            public void elementRemoved(String messageId) {
                // Remove old email from map
                Set<Email> emails = messageMap.keySet();
                Iterator<Email> iter = emails.iterator();
                while (iter.hasNext()) {
                    Email email = iter.next();
                    if (email.getMessageID().equals(messageId)) {
                        iter.remove();
                        break;
                    }
                }
                updateMessages();
            }
        };
        folder.addFolderListener(folderListener);
    }

    protected void stopListening() {
        folder.removeFolderListener(folderListener);
        folderListener = null;
    }
    
    private String getFolderName() {
        return folder.getName();
    }
    
    /** Synchronizes the <code>messages</code> field from the underlying {@link EmailFolder}. */
    private void updateMessages() {
        // Generate the updated list of messages
        messages = Collections.synchronizedList(new ArrayList<BoteMessage>(messageMap.values()));
        // Update UIDs
        for (BoteMessage message : messages) {
            if (message.getUid() == 0) {
                message.setUid(uid);
                uid++;
            }
        }
    }
    
    List<BoteMessage> getAllMessages() {
        return messages;
    }
    
    List<Message<String>> getMessages(MessageRange set, int limit) {
        List<Message<String>> messageList = new ArrayList<Message<String>>();
        for (long index: set) {
            if (index < 1)   // IMAP indices start at 1
                continue;
            if (index > messages.size())
                break;
            BoteMessage message = messages.get((int)index - 1);
            messageList.add(message);
        }
        return messageList;
    }
    
    int getNumMessages() {
        return messages.size();
    }
    
    void add(BoteMessage message) throws IOException, MessagingException, PasswordException, GeneralSecurityException {
        folder.add(message.getEmail());
    }
    
    void saveMetadata(BoteMessage message) throws IOException, MessagingException, PasswordException, GeneralSecurityException {
        folder.saveMetadata(message.getEmail());
        modSeq++;
    }
    
    EmailFolder getFolder() {
        return folder;
    }
    
    long getUid() {
        return uid;
    }

    long getModSeq() {
        return modSeq;
    }

    @Override
    public String toString() {
        return folder.toString();
    }
}
