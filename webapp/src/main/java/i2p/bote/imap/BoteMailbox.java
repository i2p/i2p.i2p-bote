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

import com.google.common.base.Optional;

import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;

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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.mail.Flags.Flag;
import javax.mail.MessagingException;

import i2p.bote.email.Email;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.FolderListener;

import static i2p.bote.web.WebappUtil._t;

/**
 * Implementation of {@link org.apache.james.mailbox.store.mail.model.Mailbox}.
 */
public class BoteMailbox extends SimpleMailbox {
    private EmailFolder folder;
    private SortedMap<Email, BoteMessage> messageMap;
    private List<BoteMessage> messages;
    private MessageUid lastUid;
    private MessageUid nextUid;
    private final ReadWriteLock nextUidLock = new ReentrantReadWriteLock();
    private long highestModSeq;
    private long nextModSeq;
    private final ReadWriteLock nextModSeqLock = new ReentrantReadWriteLock();
    private FolderListener folderListener;

    public BoteMailbox(EmailFolder folder, long uidValidity, MessageUid nextUid) {
        super(new MailboxPath(MailboxConstants.USER_NAMESPACE, "bote", folder.getName()), uidValidity,
                new BoteMailboxId(folder.getName()));
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
        nextUidLock.writeLock().lock();
        try {
            this.nextUid = nextUid;
        } finally {
            nextUidLock.writeLock().unlock();
        }

        nextModSeqLock.writeLock().lock();
        try {
            nextModSeq = System.currentTimeMillis();
        } finally {
            nextModSeqLock.writeLock().unlock();
        }

        startListening();
        try {
            List<Email> emails = folder.getElements();
            for (Email email : emails)
                messageMap.put(email, new BoteMessage(email, getMailboxId()));
            updateMessages();
        } catch (PasswordException e) {
            throw new RuntimeException(_t("Password required or invalid password provided"), e);
        }
    }

    protected void startListening() {
        folderListener = new FolderListener() {
            public void elementAdded(String messageId) {
                try {
                    // Add new emails to map
                    Email email = folder.getEmail(messageId);
                    email.setFlag(Flag.RECENT, true);
                    messageMap.put(email, new BoteMessage(email, getMailboxId()));
                    updateMessages();
                } catch (PasswordException e) {
                    throw new RuntimeException(_t("Password required or invalid password provided"), e);
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
    
    /** Synchronizes the <code>messages</code> field from the underlying {@link EmailFolder}. */
    private void updateMessages() {
        // Generate the updated list of messages
        messages = Collections.synchronizedList(new ArrayList<BoteMessage>(messageMap.values()));
        // Update UIDs
        for (BoteMessage message : messages) {
            if (message.getUid() == null) {
                nextUidLock.writeLock().lock();
                try {
                    MessageUid curUid = nextUid;
                    nextUid = nextUid.next();
                    message.setUid(curUid);
                    lastUid = curUid;
                } finally {
                    nextUidLock.writeLock().unlock();
                }
            }
        }
    }
    
    List<BoteMessage> getAllMessages() {
        return messages;
    }
    
    List<MailboxMessage> getMessages(MessageRange set, int limit) {
        List<MailboxMessage> messageList = new ArrayList<>();
        for (BoteMessage message: messages) {
            if (limit >= 0 && messageList.size() >= limit)
                break;
            if (set.includes(message.getUid()))
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
        nextModSeqLock.writeLock().lock();
        try {
            folder.saveMetadata(message.getEmail());
            highestModSeq = nextModSeq;
            nextModSeq++;
        } finally {
            nextModSeqLock.writeLock().unlock();
        }
    }
    
    EmailFolder getFolder() {
        return folder;
    }

    void lockNextUid(boolean writeLock) {
        if (writeLock) {
            nextUidLock.writeLock().lock();
        } else {
            nextUidLock.readLock().lock();
        }
    }

    MessageUid lockedNextUid() {
        return nextUid;
    }

    void unlockNextUid(boolean writeLock) {
        if (writeLock) {
            nextUidLock.writeLock().unlock();
        } else {
            nextUidLock.readLock().unlock();
        }
    }

    Optional<MessageUid> lastUid() {
        return Optional.fromNullable(lastUid);
    }

    void lockNextModSeq(boolean writeLock) {
        if (writeLock) {
            nextModSeqLock.writeLock().lock();
        } else {
            nextModSeqLock.readLock().lock();
        }
    }

    long lockedNextModSeq() {
        return nextModSeq;
    }

    void unlockNextModSeq(boolean writeLock) {
        if (writeLock) {
            nextModSeqLock.writeLock().unlock();
        } else {
            nextModSeqLock.readLock().unlock();
        }
    }

    long highestModSeq() {
        return highestModSeq;
    }

    @Override
    public String toString() {
        return folder.toString();
    }
}
