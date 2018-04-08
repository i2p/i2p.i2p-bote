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

import net.i2p.util.Log;

import org.apache.james.mailbox.MailboxPathLocker;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.exception.SubscriptionException;
import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.MessageMetaData;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.store.AbstractMailboxPathLocker;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.SimpleMessageMetaData;
import org.apache.james.mailbox.store.mail.AbstractLockingModSeqProvider;
import org.apache.james.mailbox.store.mail.AbstractLockingUidProvider;
import org.apache.james.mailbox.store.mail.AbstractMessageMapper;
import org.apache.james.mailbox.store.mail.AnnotationMapper;
import org.apache.james.mailbox.store.mail.AttachmentMapper;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.MessageIdMapper;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.ModSeqProvider;
import org.apache.james.mailbox.store.mail.UidProvider;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.user.SubscriptionMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.Flags;
import javax.mail.Flags.Flag;

import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.EmailFolderManager;

import static i2p.bote.web.WebappUtil._t;

/**
 * Concrete subclass of {@link MailboxSessionMapperFactory}.
 */
class MapperFactory extends MailboxSessionMapperFactory {
    private Log log = new Log(MapperFactory.class);
    private EmailFolderManager folderManager;
    private final Map<String, BoteMailbox> mailboxes;
    private long uidValidity;
    private UidProvider uidProvider;
    private ModSeqProvider modSeqProvider;
    
    MapperFactory(EmailFolderManager folderManager) {
        this.folderManager = folderManager;
        mailboxes = new HashMap<>();
        uidValidity = System.currentTimeMillis();
        uidProvider = createUidProvider();
        modSeqProvider = createModSeqProvider();
    }

    @Override
    public UidProvider getUidProvider() {
        return uidProvider;
    }

    @Override
    public ModSeqProvider getModSeqProvider() {
        return modSeqProvider;
    }

    @Override
    public SubscriptionMapper createSubscriptionMapper(MailboxSession session) throws SubscriptionException {
        // not implemented for now
        return null;
    }

    /** Maps mailbox names to mailboxes. */
    private Map<String, BoteMailbox> getMailboxes() {
        // XXX: When user folders are set up, this will need to be updated.
        if (mailboxes.isEmpty()) {
            for (EmailFolder folder: folderManager.getEmailFolders()) {
                String folderName = folder.getName().toLowerCase();
                mailboxes.put(folderName, new BoteMailbox(folder, uidValidity, MessageUid.MIN_VALUE));
            }
        }
        return mailboxes;
    }

    protected void stopListening() {
        for (BoteMailbox mailbox : mailboxes.values()) {
            mailbox.stopListening();
        }
    }

    @Override
    public AnnotationMapper createAnnotationMapper(MailboxSession session) throws MailboxException {
        return null;
    }

    @Override
    public MessageMapper createMessageMapper(MailboxSession mailboxSession) throws MailboxException {
        return new AbstractMessageMapper(mailboxSession, uidProvider, modSeqProvider) {

            @Override
            public long countMessagesInMailbox(Mailbox mailbox) throws MailboxException {
                String mailboxName = mailbox.getName().toLowerCase();
                BoteMailbox boteMailbox = getMailboxes().get(mailboxName);
                return boteMailbox==null ? -1 : boteMailbox.getNumMessages();
            }

            @Override
            public long countUnseenMessagesInMailbox(Mailbox mailbox) throws MailboxException {
                int count = 0;
                BoteMailbox boteMailbox = (BoteMailbox)mailbox;
                for (BoteMessage message: boteMailbox.getAllMessages())
                    if (!message.isSeen())
                        count++;
                return count;
            }

            @Override
            public void delete(Mailbox mailbox, MailboxMessage message) throws MailboxException {
                EmailFolder folder = ((BoteMailbox)mailbox).getFolder();
                MessageId messageId = message.getMessageId();
                boolean deleted = folderManager.deleteEmail(folder, messageId.serialize());
                if (!deleted)
                    log.error("Can't delete Message " + messageId + " from folder " + folder);
            }

            @Override
            public Map<MessageUid, MessageMetaData> expungeMarkedForDeletionInMailbox(Mailbox mailbox, MessageRange set) throws MailboxException {
                final Map<MessageUid, MessageMetaData> filteredResult = new HashMap<>();

                Iterator<MailboxMessage> it = findInMailbox(mailbox, set, FetchType.Metadata, -1);
                while (it.hasNext()) {
                    MailboxMessage message = it.next();
                    if (message.isDeleted()) {
                        filteredResult.put(message.getUid(), new SimpleMessageMetaData(message));
                        delete(mailbox, message);
                    }
                }

                return filteredResult;
            }

            @Override
            public MessageUid findFirstUnseenMessageUid(Mailbox mailbox) throws MailboxException {
                BoteMailbox boteMailbox = (BoteMailbox)mailbox;
                for (BoteMessage message: boteMailbox.getAllMessages())
                    if (!message.isSeen())
                        return message.getUid();
                return null;
            }

            @Override
            public Iterator<MailboxMessage> findInMailbox(Mailbox mailbox, MessageRange set, FetchType type, int limit) throws MailboxException {
                BoteMailbox boteBox = (BoteMailbox)mailbox;
                return boteBox.getMessages(set, limit).iterator();
            }

            @Override
            public List<MessageUid> findRecentMessageUidsInMailbox(Mailbox mailbox) throws MailboxException {
                final List<MessageUid> results = new ArrayList<>();
                BoteMailbox boteMailbox = (BoteMailbox)mailbox;
                for (BoteMessage message: boteMailbox.getAllMessages())
                    if (message.isRecent())
                        results.add(message.getUid());
                Collections.sort(results);
                return results;
            }

            @Override
            public void endRequest() {
            }

            @Override
            public MessageMetaData copy(Mailbox mailbox, MessageUid uid, long modSeq, MailboxMessage original) throws MailboxException {
                BoteMailbox boteBox = (BoteMailbox)mailbox;
                try {
                    BoteMessage newMessage = new BoteMessage((BoteMessage)original);
                    newMessage.setUid(uid);
                    newMessage.setModSeq(modSeq);
                    Flags flags = original.createFlags();

                    // Mark message as recent as it is a copy
                    flags.add(Flag.RECENT);
                    newMessage.setFlags(flags);
                    boteBox.add(newMessage);
                    return new SimpleMessageMetaData(newMessage);
                } catch (Exception e) {
                    throw new MailboxException("Can't copy message [" + original + "] to folder [" + mailbox + "]");
                }
            }

            @Override
            public MessageMetaData move(Mailbox mailbox, MailboxMessage message) throws MailboxException {
                MessageMetaData metadata = copy(mailbox, message);
                delete(mailbox, message);
                return metadata;
            }

            @Override
            public Flags getApplicableFlag(Mailbox mailbox) throws MailboxException {
                Flags flags = new Flags();
                flags.add(Flag.ANSWERED);
                flags.add(Flag.DELETED);
                flags.add(Flag.RECENT);
                flags.add(Flag.SEEN);
                return flags;
            }

            /** Updates the metadata */
            @Override
            protected MessageMetaData save(Mailbox mailbox, MailboxMessage message) throws MailboxException {
                // Ignore requests to save non-BoteMessages so email clients don't save messages to the sent folder
                // (I2P-Bote does this already, plus the email shouldn't show up in "sent" until it's actually been sent to the DHT)
                if (!(message instanceof BoteMessage))
                    return new SimpleMessageMetaData(message);
                
                BoteMailbox boteBox = (BoteMailbox)mailbox;
                BoteMessage boteMessage = (BoteMessage)message;
                try {
                    boteBox.saveMetadata(boteMessage);
                    return new SimpleMessageMetaData(message);
                } catch (Exception e) {
                    throw new MailboxException("Can't add message to folder " + boteBox.getFolder(), e);
                }
            }

            @Override
            protected void begin() throws MailboxException {
                // not implemented for now
            }

            @Override
            protected void commit() throws MailboxException {
                // not implemented for now
            }

            @Override
            protected void rollback() throws MailboxException {
                // not implemented for now
            }
        };
    }

    @Override
    public MessageIdMapper createMessageIdMapper(MailboxSession session) throws MailboxException {
        return null;
    }

    @Override
    public AttachmentMapper createAttachmentMapper(MailboxSession session) throws MailboxException {
        return null;
    }

    private UidProvider createUidProvider() {
        MailboxPathLocker uidLocker = new AbstractMailboxPathLocker() {
            @Override
            protected void lock(MailboxSession session, MailboxPath path, boolean writeLock) throws MailboxException {
                BoteMailbox mailbox = getMailboxes().get(path.getName().toLowerCase());
                mailbox.lockNextUid(writeLock);
            }

            @Override
            protected void unlock(MailboxSession session, MailboxPath path, boolean writeLock) throws MailboxException {
                BoteMailbox mailbox = getMailboxes().get(path.getName().toLowerCase());
                mailbox.unlockNextUid(writeLock);
            }
        };

        return new AbstractLockingUidProvider(uidLocker) {
            @Override
            protected MessageUid lockedNextUid(MailboxSession session, Mailbox mailbox) throws MailboxException {
                BoteMailbox boteMailbox = (BoteMailbox)mailbox;
                return boteMailbox.lockedNextUid();
            }

            @Override
            public Optional<MessageUid> lastUid(MailboxSession session, Mailbox mailbox) throws MailboxException {
                BoteMailbox boteMailbox = (BoteMailbox)mailbox;
                return boteMailbox.lastUid();
            }
        };
    }

    private ModSeqProvider createModSeqProvider() {
        MailboxPathLocker modSeqLocker = new AbstractMailboxPathLocker() {
            @Override
            protected void lock(MailboxSession session, MailboxPath path, boolean writeLock) throws MailboxException {
                BoteMailbox mailbox = getMailboxes().get(path.getName().toLowerCase());
                mailbox.lockNextModSeq(writeLock);
            }

            @Override
            protected void unlock(MailboxSession session, MailboxPath path, boolean writeLock) throws MailboxException {
                BoteMailbox mailbox = getMailboxes().get(path.getName().toLowerCase());
                mailbox.unlockNextModSeq(writeLock);
            }
        };

        return new AbstractLockingModSeqProvider(modSeqLocker) {
            @Override
            protected long lockedNextModSeq(MailboxSession session, Mailbox mailbox) throws MailboxException {
                BoteMailbox boteMailbox = (BoteMailbox)mailbox;
                return boteMailbox.lockedNextModSeq();
            }

            @Override
            public long highestModSeq(MailboxSession session, Mailbox mailbox) throws MailboxException {
                BoteMailbox boteMailbox = (BoteMailbox)mailbox;
                return boteMailbox.highestModSeq();
            }

            @Override
            public long highestModSeq(MailboxSession session, MailboxId mailboxId) throws MailboxException {
                return highestModSeq(session, getMailboxes().get(mailboxId.serialize().toLowerCase()));
            }
        };
    }

    @Override
    public MailboxMapper createMailboxMapper(MailboxSession session) throws MailboxException {
        return new MailboxMapper() {
            
            @Override
            public <T> T execute(Transaction<T> transaction) throws MailboxException {
                return transaction.run();
            }

            @Override
            public void endRequest() {
                // nothing to do
            }

            @Override
            public MailboxId save(Mailbox mailbox) throws MailboxException {
                // nothing to do because changes are written to disk immediately
                return mailbox.getMailboxId();
            }

            @Override
            public List<Mailbox> list() throws MailboxException {
                return new ArrayList<Mailbox>(getMailboxes().values());
            }

            @Override
            public boolean hasChildren(Mailbox mailbox, char delimiter) throws MailboxException, MailboxNotFoundException {
                return false;  // not currently supported
            }

            @Override
            public List<Mailbox> findMailboxWithPathLike(MailboxPath mailboxPath) throws MailboxException {
                String regex = mailboxPath.getName().replaceAll("%", ".*");
                
                List<Mailbox> results = new ArrayList<>();
                for (BoteMailbox mailbox: getMailboxes().values()) {
                    String mailboxName = mailbox.getName().toLowerCase();
                    if (mailboxName.matches(regex))
                        results.add(mailbox);
                }
                return results;
            }

            @Override
            public Mailbox findMailboxByPath(MailboxPath mailboxName) throws MailboxException, MailboxNotFoundException {
                return getMailboxes().get(mailboxName.getName().toLowerCase());
            }

            @Override
            public Mailbox findMailboxById(MailboxId mailboxId) throws MailboxException, MailboxNotFoundException {
                // Assumes mailboxId is a BoteMailboxId, which is serialized to its name
                return getMailboxes().get(mailboxId.serialize().toLowerCase());
            }

            @Override
            public void delete(Mailbox mailbox) throws MailboxException {
                throw new MailboxException(_t("Deletion of mailboxes is not currently supported."));
            }

            @Override
            public void updateACL(Mailbox mailbox, MailboxACL.MailboxACLCommand mailboxACLCommand)
                    throws MailboxException {
                mailbox.setACL(mailbox.getACL().apply(mailboxACLCommand));
            }
        };
    }
}
