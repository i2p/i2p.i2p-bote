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
import java.util.List;

import javax.mail.MessagingException;

import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.store.mail.model.Message;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;

/**
 * Implementation of {@link org.apache.james.mailbox.store.mail.model.Mailbox}.
 */
public class BoteMailbox extends SimpleMailbox<String> {
    private EmailFolder folder;
    private List<BoteMessage> messages;
    private Long uid;
    private long modSeq;

    public BoteMailbox(EmailFolder folder, long uidValidity, long uid) {
        super(new MailboxPath("I2P-Bote", "bote", folder.getName()), uidValidity);
        this.folder = folder;
        this.uid = uid;
        
        modSeq = System.currentTimeMillis();
        modSeq <<= 32;
        
        folder.addFolderListener(new FolderListener() {
            public void elementRemoved() {
                updateMessages();
            }
            
            public void elementAdded() {
                updateMessages();
            }
        });
        updateMessages();
    }
    
    private String getFolderName() {
        return folder.getName();
    }
    
    /** Synchronizes the <code>messages</code> field from the underlying {@link EmailFolder}. */
    private void updateMessages() {
        try {
            List<Email> emails = folder.getElements();
            messages = new ArrayList<BoteMessage>();
            for (Email email: emails)
                messages.add(new BoteMessage(email, getFolderName()));
        } catch (PasswordException e) {
            throw new RuntimeException(_("Password required or invalid password provided"), e);
        }
        Collections.sort(messages, new Comparator<BoteMessage>() {
            @Override
            public int compare(BoteMessage message1, BoteMessage message2) {
                return message1.getMessageID().compareTo(message2.getMessageID());
            }
        });
        for (int i=0; i<messages.size(); i++)
            messages.get(i).setUid(i + 1);
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