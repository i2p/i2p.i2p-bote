package i2p.bote.imap;

import org.apache.james.mailbox.model.MessageId;

import i2p.bote.UniqueId;

class BoteMessageId implements MessageId {
    private final String mid;

    BoteMessageId(String mid) {
        this.mid = mid;
    }

    static class Factory implements MessageId.Factory {
        @Override
        public MessageId fromString(String serialized) {
            return new BoteMessageId(serialized);
        }

        @Override
        public MessageId generate() {
            return new BoteMessageId(new UniqueId().toBase64());
        }
    }

    @Override
    public String serialize() {
        return mid;
    }
}
