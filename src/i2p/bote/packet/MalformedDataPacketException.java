package i2p.bote.packet;

public class MalformedDataPacketException extends Exception {
    private static final long serialVersionUID = -600763395717614292L;

    public MalformedDataPacketException(String message, Throwable cause) {
        super(message, cause);
    }
}