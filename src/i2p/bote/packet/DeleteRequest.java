package i2p.bote.packet;

public abstract class DeleteRequest extends CommunicationPacket {

    protected DeleteRequest() {
    }
    
    protected DeleteRequest(byte[] data) {
        super(data);
    }
    
    public abstract Class<? extends I2PBotePacket> getDataType();
}