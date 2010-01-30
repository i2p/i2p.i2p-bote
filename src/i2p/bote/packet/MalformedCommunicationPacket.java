package i2p.bote.packet;

/**
 * Represents an invalid packet that a peer sent.
 */
public class MalformedCommunicationPacket extends CommunicationPacket {
    
    public MalformedCommunicationPacket() {
    }

    @Override
    public byte[] toByteArray() {
        throw new UnsupportedOperationException();
    }
}