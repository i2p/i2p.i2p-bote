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

package i2p.bote.email;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import net.i2p.data.DataFormatException;
import net.i2p.data.DataHelper;
import net.i2p.data.Hash;
import net.i2p.util.Log;

/**
 * Contains data about an email.<br/>
 * Metadata is not stored in mail headers because:<br/>
 * <ul>
 *   <li/>Metadata may change after the email has been sent or received.
 *        Rewriting a small metadata file is more efficient than a
 *        potentially large email file.
 *   <li/>If the metadata were stored in mail headers, it would have to
 *        be excluded when verifying the email signature via
 *        {@link Email#isSignatureValid()}. Having a list of headers to
 *        exclude would create unnecessary dependencies between
 *        {@link Email} and any code that makes changes to metadata.
 * </ul>
 * This class is not thread-safe.
 */
public class EmailMetadata extends Properties {
    private static final long serialVersionUID = 9058161682262839810L;
    
    private Log log = new Log(EmailMetadata.class);
    
    public EmailMetadata() {
        setNew(true);
    }
    
    public EmailMetadata(File file) throws IOException {
        this();
        DataHelper.loadProps(this, file);
    }
    
    /**
     * Sets the time and date the email was received.
     * @param receivedDate
     */
    public void setReceivedDate(Date receivedDate) {
        String dateStr = String.valueOf(receivedDate.getTime());
        setProperty("receivedDate", dateStr);
    }
    
    /**
     * Returns the time and date the email was received. If no received
     * date has been set, or if it is invalid, <code>null</code> is returned.
     */
    public Date getReceivedDate() {
        String dateStr = getProperty("receivedDate");
        if (dateStr == null)
            return null;
        else {
            try {
                long milliseconds = Long.valueOf(dateStr);
                return new Date(milliseconds);
            }
            catch (NumberFormatException e) {
                log.error("Invalid received date (should be a whole number): <" + dateStr + ">");
                return null;
            }
        }
    }
    
    public void setNew(boolean isNew) {
        setProperty("new", String.valueOf(isNew));
    }

    /**
     * Returns <code>true</code> if the email is unread (incoming mail), or
     * if it has not been sent yet (outgoing mail).<br/>
     * The default is <code>true</code>.
     */
    public boolean isNew() {
        return getProperty("new").equalsIgnoreCase("true");
    }

    public void addPacketPendingDelivery(EmailDestination destination, Hash dhtKey, Hash delVerificationHash) {
        int destIndex = getDestinationIndex(destination);
        int packetIndex = getPacketIndex(destIndex, dhtKey);
        setProperty("destination" + destIndex, destination.toBase64());
        setProperty("destination" + destIndex + ".packet" + packetIndex, dhtKey.toBase64());
        setProperty("destination" + destIndex + ".delVerifHash" + packetIndex, delVerificationHash.toBase64());
        setProperty("destination" + destIndex + ".deletedFromDht" + packetIndex, "false");
    }
    
    private int getDestinationIndex(EmailDestination destination) {
        int destIndex = 0;
        while (true) {
            String value = getProperty("destination" + destIndex);
            if (value==null || destination.toBase64().equals(value))
                return destIndex;
            destIndex++;
        }
    }
    
    private int getPacketIndex(int destIndex, Hash dhtKey) {
        int pktIndex = 0;
        while (true) {
            String value = getProperty("destination" + destIndex + ".packet" + pktIndex);
            if (value==null || dhtKey.toBase64().equals(value))
                return pktIndex;
            pktIndex++;
        }
    }
    
    public Hash getDeleteVerificationHash(EmailDestination destination, Hash dhtKey) {
        int destIndex = getDestinationIndex(destination);
        int packetIndex = getPacketIndex(destIndex, dhtKey);
        String hashStr = getProperty("destination" + destIndex + ".delVerifHash" + packetIndex);
        Hash hash = new Hash();
        try {
            hash.fromBase64(hashStr);
            return hash;
        }
        catch (DataFormatException e) {
            log.error("Invalid delete verification hash: <" + hashStr + ">", e);
            return null;
        }
    }

    public void setPacketDelivered(EmailDestination destination, Hash dhtKey, boolean delivered) {
        int destIndex = getDestinationIndex(destination);
        int packetIndex = getPacketIndex(destIndex, dhtKey);
        setProperty("destination" + destIndex + ".deletedFromDht" + packetIndex, "true");
    }
    
    public int getNumUndeliveredRecipients() {
        int destIndex = 0;
        int numUndelivered = 0;
        while (true) {
            String value = getProperty("destination" + destIndex);
            if (value == null)
                break;
            if (!isDelivered(destIndex))
                numUndelivered++;
            destIndex++;
        }
        return numUndelivered;
    }
    
    private boolean isDelivered(int destIndex) {
        int packetIndex = 0;
        while (true) {
            String packetDeliveredStr = getProperty("destination" + destIndex + ".deletedFromDht" + packetIndex);
            if (packetDeliveredStr == null)
                return true;
            boolean packetDelivered = "true".equalsIgnoreCase(packetDeliveredStr);
            if (!packetDelivered)
                return false;

            packetIndex++;
        }
    }
    
    public void writeTo(File file) throws IOException {
        try {
            DataHelper.storeProps(this, file);
        }
        catch (IOException e) {
            log.error("Can't write metadata to file: <" + file + ">", e);
        }
    }
}