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
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.EmailFolderManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.Flags;
import javax.mail.Flags.Flag;

import net.i2p.util.Log;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.exception.SubscriptionException;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageMetaData;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.SimpleMessageMetaData;
import org.apache.james.mailbox.store.mail.AbstractMessageMapper;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.ModSeqProvider;
import org.apache.james.mailbox.store.mail.UidProvider;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.user.SubscriptionMapper;

/**
 * Concrete subclass of {@link MailboxSessionMapperFactory}.
 */
class MapperFactory extends MailboxSessionMapperFactory<String> {
    private Log log = new Log(MapperFactory.class);
    private EmailFolderManager folderManager;
    private final Map<String, BoteMailbox> mailboxes;
    private long uidValidity;
    
    MapperFactory(EmailFolderManager folderManager) {
        this.folderManager = folderManager;
        mailboxes = new HashMap<String, BoteMailbox>();
        uidValidity = System.currentTimeMillis();
        uidValidity <<= 32;
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
                mailboxes.put(folderName, new BoteMailbox(folder, uidValidity, 1));
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
    public MessageMapper<String> createMessageMapper(MailboxSession mailboxSession) throws MailboxException {
        UidProvider<String> uidProvider = createUidProvider();
        ModSeqProvider<String> modSeqProvider = createModSeqProvider();
        
        return new AbstractMessageMapper<String>(mailboxSession, uidProvider, modSeqProvider) {

            @Override
            public long countMessagesInMailbox(Mailbox<String> mailbox) throws MailboxException {
                String mailboxName = mailbox.getName().toLowerCase();
                BoteMailbox boteMailbox = getMailboxes().get(mailboxName);
                return boteMailbox==null ? -1 : boteMailbox.getNumMessages();
            }

            @Override
            public long countUnseenMessagesInMailbox(Mailbox<String> mailbox) throws MailboxException {
                int count = 0;
                BoteMailbox boteMailbox = (BoteMailbox)mailbox;
                for (BoteMessage message: boteMailbox.getAllMessages())
                    if (!message.isSeen())
                        count++;
                return count;
            }

            @Override
            public void delete(Mailbox<String> mailbox, Message<String> message) throws MailboxException {
                EmailFolder folder = ((BoteMailbox)mailbox).getFolder();
                String messageId = ((BoteMessage)message).getMessageID();
                boolean deleted = folderManager.deleteEmail(folder, messageId);
                if (!deleted)
                    log.error("Can't delete Message " + messageId + " from folder " + folder);
            }

            @Override
            public Map<Long, MessageMetaData> expungeMarkedForDeletionInMailbox(Mailbox<String> mailbox, MessageRange set) throws MailboxException {
                final Map<Long, MessageMetaData> filteredResult = new HashMap<Long, MessageMetaData>();

                Iterator<Message<String>> it = findInMailbox(mailbox, set, FetchType.Metadata, -1);
                while (it.hasNext()) {
                    Message<String> message = it.next();
                    if (message.isDeleted()) {
                        filteredResult.put(message.getUid(), new SimpleMessageMetaData(message));
                        delete(mailbox, message);
                    }
                }

                return filteredResult;
            }

            @Override
            public Long findFirstUnseenMessageUid(Mailbox<String> mailbox) throws MailboxException {
                BoteMailbox boteMailbox = (BoteMailbox)mailbox;
                for (BoteMessage message: boteMailbox.getAllMessages())
                    if (!message.isSeen())
                        return message.getUid();
                return null;
            }

            @Override
            public Iterator<Message<String>> findInMailbox(Mailbox<String> mailbox, MessageRange set, FetchType type, int limit) throws MailboxException {
                BoteMailbox boteBox = (BoteMailbox)mailbox;
                return boteBox.getMessages(set, limit).iterator();
            }

            @Override
            public List<Long> findRecentMessageUidsInMailbox(Mailbox<String> mailbox) throws MailboxException {
                final List<Long> results = new ArrayList<Long>();
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
            protected MessageMetaData copy(Mailbox<String> mailbox, long uid, long modSeq, Message<String> original) throws MailboxException {
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
            public MessageMetaData move(Mailbox<String> mailbox, Message<String> message) throws MailboxException {
                MessageMetaData metadata = copy(mailbox, message);
                delete(mailbox, message);
                return metadata;
            }

            /** Updates the metadata */
            @Override
            protected MessageMetaData save(Mailbox<String> mailbox, Message<String> message) throws MailboxException {
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

    private UidProvider<String> createUidProvider() {
        return new UidProvider<String>() {
            @Override
            public long lastUid(MailboxSession session, Mailbox<String> mailbox) throws MailboxException {
                BoteMailbox boteMailbox = (BoteMailbox)mailbox;
                return boteMailbox.getUid();
            }

            @Override
            public long nextUid(MailboxSession session, Mailbox<String> mailbox) throws MailboxException {
                return lastUid(session, mailbox) + 1;
            }
        };
    }

    private ModSeqProvider<String> createModSeqProvider() {
        return new ModSeqProvider<String>() {
            @Override
            public long highestModSeq(MailboxSession session, Mailbox<String> mailbox) throws MailboxException {
                BoteMailbox boteMailbox = (BoteMailbox)mailbox;
                return boteMailbox.getModSeq();
            }

            @Override
            public long nextModSeq(MailboxSession session, Mailbox<String> mailbox) throws MailboxException {
                return highestModSeq(session, mailbox) + 1;
            }
        };
    }
    
    @Override
    public MailboxMapper<String> createMailboxMapper(MailboxSession session) throws MailboxException {
        return new MailboxMapper<String>() {
            
            @Override
            public <T> T execute(Transaction<T> transaction) throws MailboxException {
                return transaction.run();
            }

            @Override
            public void endRequest() {
                // nothing to do
            }

            @Override
            public void save(Mailbox<String> mailbox) throws MailboxException {
                // nothing to do because changes are written to disk immediately
            }

            @Override
            public List<Mailbox<String>> list() throws MailboxException {
                return new ArrayList<Mailbox<String>>(getMailboxes().values());
            }

            @Override
            public boolean hasChildren(Mailbox<String> mailbox, char delimiter) throws MailboxException, MailboxNotFoundException {
                return false;  // not currently supported
            }

            @Override
            public List<Mailbox<String>> findMailboxWithPathLike(MailboxPath mailboxPath) throws MailboxException {
                String regex = mailboxPath.getName().replaceAll("%", ".*");
                
                List<Mailbox<String>> results = new ArrayList<Mailbox<String>>();
                for (BoteMailbox mailbox: getMailboxes().values()) {
                    String mailboxName = mailbox.getName().toLowerCase();
                    if (mailboxName.matches(regex))
                        results.add(mailbox);
                }
                return results;
            }

            @Override
            public Mailbox<String> findMailboxByPath(MailboxPath mailboxName) throws MailboxException, MailboxNotFoundException {
                return getMailboxes().get(mailboxName.getName().toLowerCase());
            }

            @Override
            public void delete(Mailbox<String> mailbox) throws MailboxException {
                throw new MailboxException(_("Deletion of mailboxes is not currently supported."));
            }
        };
    }
}