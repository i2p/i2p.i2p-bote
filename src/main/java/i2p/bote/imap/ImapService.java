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

import i2p.bote.Configuration;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.fileencryption.PasswordVerifier;
import i2p.bote.folder.EmailFolder;
import i2p.bote.folder.EmailFolderManager;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.mail.Flags;

import net.i2p.util.Log;
import nl.jteam.tls.StrongTls;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.imap.api.display.HumanReadableText;
import org.apache.james.imap.api.display.Locales;
import org.apache.james.imap.api.display.Localizer;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.encode.main.DefaultImapEncoderFactory;
import org.apache.james.imap.main.DefaultImapDecoderFactory;
import org.apache.james.imap.processor.main.DefaultImapProcessorFactory;
import org.apache.james.imapserver.netty.IMAPServer;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.SubscriptionManager;
import org.apache.james.mailbox.acl.GroupMembershipResolver;
import org.apache.james.mailbox.acl.MailboxACLResolver;
import org.apache.james.mailbox.exception.SubscriptionException;
import org.apache.james.mailbox.exception.UnsupportedRightException;
import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.model.MailboxACL.MailboxACLEntryKey;
import org.apache.james.mailbox.model.MailboxACL.MailboxACLRight;
import org.apache.james.mailbox.model.MailboxACL.MailboxACLRights;
import org.apache.james.mailbox.model.SimpleMailboxACL;
import org.apache.james.mailbox.store.Authenticator;
import org.apache.james.mailbox.store.HashMapDelegatingMailboxListener;
import org.apache.james.mailbox.store.RandomMailboxSessionIdGenerator;
import org.apache.james.mailbox.store.StoreMailboxManager;
import org.slf4j.LoggerFactory;

/**
 * IMAP implementation for I2P-Bote using <a href="http://james.apache.org/">
 * Apache James</a>.
 */
public class ImapService extends IMAPServer {
    private final static String IMAP_USER = "bote";
    
    private Log log = new Log(ImapService.class);
    private EmailFolderManager folderManager;
    private MapperFactory mailboxSessionMapperFactory;

    public ImapService(Configuration configuration, final PasswordVerifier passwordVerifier, EmailFolderManager folderManager) throws ConfigurationException {
        this.folderManager = folderManager;
        
        setLog(LoggerFactory.getLogger(ImapService.class));

        HierarchicalConfiguration cfg = new HierarchicalConfiguration();
        // enable STARTTLS
        cfg.setProperty("tls.[@startTLS]", true);
        cfg.setProperty("tls.keystore", configuration.getSSLKeyStoreFile().getAbsolutePath());
        cfg.setProperty("tls.secret", configuration.getSSLKeyStorePassword());
        // select strong cipher suites
        cfg.setProperty("tls.supportedCipherSuites.cipherSuite", StrongTls.ENABLED_CIPHER_SUITES);

        configure(cfg);   // use the defaults for the rest
        setListenAddresses(new InetSocketAddress(configuration.getImapAddress(), configuration.getImapPort()));

        mailboxSessionMapperFactory = new MapperFactory(folderManager);
        MailboxACLResolver aclResolver = createMailboxACLResolver();
        GroupMembershipResolver groupMembershipResolver = new GroupMembershipResolver() {
            public boolean isMember(String user, String group) {
                return true;
            }
        };
        Authenticator authenticator = createAuthenticator(passwordVerifier);
        StoreMailboxManager<String> mailboxManager = new StoreMailboxManager<String>(mailboxSessionMapperFactory, authenticator, aclResolver, groupMembershipResolver);
        mailboxManager.setDelegatingMailboxListener(new HashMapDelegatingMailboxListener());
        mailboxManager.setMailboxSessionIdGenerator(new RandomMailboxSessionIdGenerator());
        
        SubscriptionManager subscriptionManager = createSubscriptionManager();
        
        ImapProcessor processor = DefaultImapProcessorFactory.createDefaultProcessor(mailboxManager, subscriptionManager);
        setImapProcessor(processor);

        setImapEncoder(DefaultImapEncoderFactory.createDefaultEncoder(new Localizer() {
            public String localize(HumanReadableText text, Locales locales) {
                return text.getDefaultValue();
            }
        }, true));
        setImapDecoder(DefaultImapDecoderFactory.createDecoder());
    }
    
    /** Creates a <code>MailboxACLResolver</code> that grants a logged in user full rights to everything */
    private MailboxACLResolver createMailboxACLResolver() {
        return new MailboxACLResolver() {
            
            @Override
            public MailboxACL applyGlobalACL(MailboxACL resourceACL, boolean resourceOwnerIsGroup) throws UnsupportedRightException {
                return SimpleMailboxACL.OWNER_FULL_ACL;
            }

            @Override
            public boolean hasRight(String requestUser, GroupMembershipResolver groupMembershipResolver, MailboxACLRight right, MailboxACL resourceACL, String resourceOwner, boolean resourceOwnerIsGroup) throws UnsupportedRightException {
                return true;
            }

            @Override
            public boolean isReadWrite(MailboxACLRights mailboxACLRights, Flags sharedFlags) throws UnsupportedRightException {
                return true;
            }

            @Override
            public MailboxACLRights[] listRights(MailboxACLEntryKey key, GroupMembershipResolver groupMembershipResolver, String resourceOwner, boolean resourceOwnerIsGroup) throws UnsupportedRightException {
                return new MailboxACLRights[] {SimpleMailboxACL.FULL_RIGHTS};
            }

            @Override
            public MailboxACLRights resolveRights(String requestUser, GroupMembershipResolver groupMembershipResolver, MailboxACL resourceACL, String resourceOwner, boolean resourceOwnerIsGroup) throws UnsupportedRightException {
                return SimpleMailboxACL.FULL_RIGHTS;
            }
        };
    }

    /**
     * Creates a <code>SubscriptionManager</code> that subscribes to all folders.
     * It does not support unsubscribing.
     */
    private SubscriptionManager createSubscriptionManager() {
        return new SubscriptionManager() {
            
            @Override
            public void startProcessingRequest(MailboxSession session) {
            }
            
            @Override
            public void endProcessingRequest(MailboxSession session) {
            }
            
            @Override
            public void unsubscribe(MailboxSession session, String mailbox) throws SubscriptionException {
            }
            
            @Override
            public Collection<String> subscriptions(MailboxSession session) throws SubscriptionException {
                Collection<String> folderNames = new ArrayList<String>();
                for (EmailFolder folder: ImapService.this.folderManager.getEmailFolders())
                    folderNames.add(folder.getName());
                return folderNames;
            }
            
            @Override
            public void subscribe(MailboxSession session, String mailbox) throws SubscriptionException {
            }
        };
    }
    
    /** Creates an <code>Authenticator</code> that checks the I2P-Bote password. */
    private Authenticator createAuthenticator(final PasswordVerifier passwordVerifier) {
        return new Authenticator() {
            
            @Override
            public boolean isAuthentic(String userid, CharSequence passwd) {
                if (!IMAP_USER.equals(userid))
                    return false;
                
                byte[] passwordBytes = passwd.toString().getBytes();
                try {
                    passwordVerifier.tryPassword(passwordBytes);
                } catch (PasswordException e) {
                    return false;
                } catch (Exception e) {
                    log.error("Can't check password", e);
                }
                
                return true;
            }
        };
    };

    /** Starts the IMAP server in a new thread and returns. */
    @Override
    public boolean start() {
        boolean started = super.start();
        log.info("IMAP service listening on " + Arrays.toString(getBoundAddresses()));
        return started;
    }

    @Override
    public boolean stop() {
        mailboxSessionMapperFactory.stopListening();
        boolean stopped = super.stop();
        return stopped;
    }
}