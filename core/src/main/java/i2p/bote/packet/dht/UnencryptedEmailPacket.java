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

package i2p.bote.packet.dht;

import i2p.bote.UniqueId;
import i2p.bote.Util;
import i2p.bote.packet.DataPacket;
import i2p.bote.packet.TypeCode;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import net.i2p.util.Log;

/**
 * Besides the email data, an <code>UnencryptedEmailPacket</code> also contains
 * a Delete Authorization key. 
 */
@TypeCode('U')
public class UnencryptedEmailPacket extends DataPacket {
    private final static int OVERHEAD = 72;   // total packet size minus content size
    
    private Log log = new Log(UnencryptedEmailPacket.class);
    private UniqueId messageId;
    private UniqueId delAuthorization;   // the unencrypted (or decrypted) delete authorization key
    private int fragmentIndex;
    private int numFragments;
    private byte[] content;

    public UnencryptedEmailPacket(InputStream inputStream) throws IOException {
        this(Util.readBytes(inputStream));
    }
    
    /**
     * Creates an <code>UnencryptedEmailPacket</code> using data from an <code>InputStream</code>
     * for the content. The number of bytes read is limited so the byte array representation of the
     * packet does not exceed <code>maxPacketSize</code>.
     * @throws IOException 
     */
    public UnencryptedEmailPacket(InputStream inputStream, UniqueId messageId, int fragmentIndex, int maxPacketSize) throws IOException {
        this.messageId = messageId;
        delAuthorization = new UniqueId();
        this.fragmentIndex = fragmentIndex;
        
        // read content
        maxPacketSize -= OVERHEAD;
        content = new byte[maxPacketSize];
        int bytesRead = inputStream.read(content);
        if (bytesRead < maxPacketSize)
            content = Arrays.copyOf(content, bytesRead);
     }
     
    /**
     * Creates an <code>UnencryptedEmailPacket</code> from a <code>byte</code> array that contains MIME data.
     * @param data
     */
    public UnencryptedEmailPacket(byte[] data) {
        super(data);
        
        ByteBuffer buffer = ByteBuffer.wrap(data, HEADER_LENGTH, data.length-HEADER_LENGTH);
        
        messageId = new UniqueId(buffer);
        delAuthorization = new UniqueId(buffer);
        fragmentIndex = buffer.getShort() & 0xFFFF;
        numFragments = buffer.getShort() & 0xFFFF;
        
        int contentLength = buffer.getShort() & 0xFFFF;
        content = new byte[contentLength];
        buffer.get(content);
        
        verify();
    }
    
    public UniqueId getMessageId() {
        return messageId;
     }
     
    public UniqueId getDeleteAuthorization() {
       return delAuthorization;
    }
    
    public int getFragmentIndex() {
       return fragmentIndex;
    }
    
    /**
     * Sets the number of packets the email was fragmented into.
     * @param numFragments
     */
    public void setNumFragments(int numFragments) {
       this.numFragments = numFragments;
       verify();
    }
    
    public int getNumFragments() {
       return numFragments;
    }
    
    /**
     * Returns the payload of the packet, which is compressed or uncompressed MIME data.
     */
    public byte[] getContent() {
       return content;
    }
    
    private void verify() {
        if (fragmentIndex<0 || fragmentIndex>=numFragments || numFragments<1)
            log.error("Illegal values: fragmentIndex=" + fragmentIndex + " numFragments="+numFragments);
        // TODO more sanity checks?
    }
    
    @Override
    public byte[] toByteArray() {
        ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(byteArrayStream);

        try {
            writeHeader(dataStream);
            messageId.writeTo(dataStream);
            delAuthorization.writeTo(dataStream);
            dataStream.writeShort(fragmentIndex);
            dataStream.writeShort(numFragments);
            dataStream.writeShort(content.length);
            dataStream.write(content);
        }
        catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
        }
        return byteArrayStream.toByteArray();
    }
    
    @Override
    public String toString() {
        return "Type=" + UnencryptedEmailPacket.class.getSimpleName() + ", msgId=" + messageId +
            ", fragIdx=" + fragmentIndex + ", numFrags=" + numFragments + ", contLen=" + content.length;
    }
}