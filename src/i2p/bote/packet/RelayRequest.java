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

package i2p.bote.packet;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

import net.i2p.I2PAppContext;
import net.i2p.client.I2PSession;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.data.PrivateKey;
import net.i2p.data.PublicKey;
import net.i2p.data.SessionKey;
import net.i2p.util.Log;

import com.nettgryppa.security.HashCash;

/**
 * A <code>RelayRequest</code> contains an encrypted {@link RelayDataPacket} or {@link DhtStorablePacket}.
 */
@TypeCode('Y')
public class RelayRequest extends CommunicationPacket {
    private static final int PADDED_SIZE = 16;   // pad to the length of an AES block (not to be confused with the AES key size)
    
    private Log log = new Log(RelayRequest.class);
    private HashCash hashCash;
    private byte[] payload;   // an encrypted DataPacket

    /**
     * Creates a <code>RelayRequest</code> that contains an encrypted <code>DataPacket</code>.
     * @param payload
     * @param destination
     */
    public RelayRequest(DataPacket payload, Destination destination) {
        try {
            hashCash = HashCash.mintCash("", 1);   // TODO
        } catch (NoSuchAlgorithmException e) {
            log.error("Cannot create HashCash.", e);
        }
        this.payload = encrypt(payload, destination);
    }
    
    public RelayRequest(byte[] data) throws MalformedDataPacketException {
        super(data);
        ByteBuffer buffer = ByteBuffer.wrap(data, HEADER_LENGTH, data.length-HEADER_LENGTH);
        
        int hashCashLength = buffer.getShort();
        byte[] hashCashData = new byte[hashCashLength];
        buffer.get(hashCashData);
        try {
            hashCash = new HashCash(new String(hashCashData));
        } catch (NoSuchAlgorithmException e) {
            log.error("Cannot create HashCash.", e);
        }
        
        int payloadLength = buffer.getShort();
        payload = new byte[payloadLength];
        buffer.get(payload);
        
        if (buffer.hasRemaining())
            log.debug("Storage Request Packet has " + buffer.remaining() + " extra bytes.");
    }

    public HashCash getHashCash() {
        return hashCash;
    }

    /**
     * Returns the payload packet, i.e. the data that is being relayed.
     * @param i2pSession An <code>I2PSession</code> that contains the private key necessary to decrypt the payload
     * @return
     * @throws DataFormatException 
     * @throws MalformedDataPacketException 
     */
    public DataPacket getStoredPacket(I2PSession i2pSession) throws DataFormatException, MalformedDataPacketException {
        return decrypt(i2pSession);
    }

    @Override
    public byte[] toByteArray() {
        ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
        DataOutputStream dataStream = new DataOutputStream(byteArrayStream);

        try {
            writeHeader(dataStream);
            String hashCashString = hashCash.toString();
            dataStream.writeShort(hashCashString.length());
            dataStream.write(hashCashString.getBytes());
            
            dataStream.writeShort(payload.length);
            dataStream.write(payload);
        }
        catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
        }
        return byteArrayStream.toByteArray();
    }
    
    private byte[] encrypt(DataPacket dataPacket, Destination destination) {
        PublicKey publicKey = destination.getPublicKey();
        I2PAppContext appContext = I2PAppContext.getGlobalContext();
        SessionKey sessionKey = appContext.sessionKeyManager().createSession(publicKey);
        byte[] data = dataPacket.toByteArray();
        return appContext.elGamalAESEngine().encrypt(data, publicKey, sessionKey, PADDED_SIZE);
    }

    /**
     * Decrypts the <code>RelayDataPacket</code> inside this packet.
     * @throws DataFormatException
     * @throws MalformedDataPacketException
     */
    private DataPacket decrypt(I2PSession i2pSession) throws DataFormatException, MalformedDataPacketException {
        PrivateKey privateKey = i2pSession.getDecryptionKey();
        I2PAppContext appContext = I2PAppContext.getGlobalContext();
        byte[] decryptedData = appContext.elGamalAESEngine().decrypt(payload, privateKey, appContext.sessionKeyManager());
        return DataPacket.createPacket(decryptedData);
    }
}