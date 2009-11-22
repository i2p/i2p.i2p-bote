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
import net.i2p.crypto.ElGamalAESEngine;
import net.i2p.data.DataFormatException;
import net.i2p.data.PrivateKey;
import net.i2p.util.Log;

import com.nettgryppa.security.HashCash;

@TypeCode('Y')
public class RelayRequest extends CommunicationPacket {
    private Log log = new Log(RelayPacket.class);
    private ElGamalAESEngine encrypter = I2PAppContext.getGlobalContext().elGamalAESEngine();
    private HashCash hashCash;
    private byte[] storedData;

    public RelayRequest(HashCash hashCash, DataPacket dataPacket) {
        this.hashCash = hashCash;
        this.storedData = dataPacket.toByteArray();
    }
    
    public RelayRequest(byte[] data) throws NoSuchAlgorithmException {
        super(data);
        ByteBuffer buffer = ByteBuffer.wrap(data, HEADER_LENGTH, data.length-HEADER_LENGTH);
        
        int hashCashLength = buffer.getShort();
        byte[] hashCashData = new byte[hashCashLength];
        buffer.get(hashCashData);
        hashCash = new HashCash(new String(hashCashData));
        
        int dataLength = buffer.getShort();
        storedData = new byte[dataLength];
        buffer.get(storedData);
        
        if (buffer.hasRemaining())
            log.debug("Storage Request Packet has " + buffer.remaining() + " extra bytes.");
    }

    public HashCash getHashCash() {
        return hashCash;
    }

    /**
     * Returns the payload packet, i.e. the data that is being relayed.
     * @param localDecryptionKey
     * @return
     * @throws DataFormatException
     */
    public DataPacket getStoredPacket(PrivateKey localDecryptionKey) throws DataFormatException {
        byte[] decryptedData = encrypter.decrypt(storedData, localDecryptionKey);
        return DataPacket.createPacket(decryptedData);
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
            dataStream.writeShort(storedData.length);
            dataStream.write(storedData);
        }
        catch (IOException e) {
            log.error("Can't write to ByteArrayOutputStream.", e);
        }
        return byteArrayStream.toByteArray();
    }
}