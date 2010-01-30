package i2p.bote.packet;

public class MalformedCommunicationPacketException extends Exception {
    private static final long serialVersionUID = 7020328715467075260L;

    public MalformedCommunicationPacketException(String message) {
        super(message);
    }
    
    public MalformedCommunicationPacketException(String message, Throwable cause) {
        super(message, cause);
    }
}