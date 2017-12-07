package i2p.bote.imap;

import org.apache.james.mailbox.model.MailboxId;

class BoteMailboxId implements MailboxId {
    private final String folderName;

    BoteMailboxId(String folderName) {
        this.folderName = folderName;
    }

    @Override
    public String serialize() {
        return folderName;
    }
}
