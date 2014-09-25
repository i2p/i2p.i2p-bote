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

import i2p.bote.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;

import javax.mail.internet.MailDateFormat;

import net.i2p.data.DataFormatException;
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
    private static final String PROPERTY_RECENT = "recent";
    private static final String PROPERTY_UNREAD = "new";
    private static final String PROPERTY_CREATE_TIME = "createTime";
    private static final String PROPERTY_RECEIVED_DATE = "receivedDate";
    private static final String PROPERTY_REPLIED = "replied";
    private static final String PROPERTY_DELETED = "deleted";
    private static final String PROPERTY_DESTINATION = "destination";
    private static final String PACKET = "packet";
    private static final String DHT_KEY = "dhtKey";
    private static final String DELETE_VERIFICATION_HASH = "delVerifHash";
    private static final String DELETED_FROM_DHT = "deletedFromDht";
    
    private Log log = new Log(EmailMetadata.class);
    
    public EmailMetadata() {
        setUnread(true);
        setCreateTime(new Date());
    }
    
    public EmailMetadata(InputStream inputStream) throws IOException {
        this();
        load(inputStream);
    }
    
    /**
     * Sets the time and date the email was received.
     * @param receivedDate
     */
    public void setReceivedDate(Date receivedDate) {
        String dateStr = String.valueOf(receivedDate.getTime());
        setProperty(PROPERTY_RECEIVED_DATE, dateStr);
    }
    
    /**
     * Returns the time and date the email was received. If no received
     * date has been set, or if it is invalid, <code>null</code> is returned.
     */
    public Date getReceivedDate() {
        String dateStr = getProperty(PROPERTY_RECEIVED_DATE);
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

    public void setRecent(boolean recent) {
        setProperty(PROPERTY_RECENT, String.valueOf(recent));
    }

    /**
     * Returns <code>true</code> if the email has recently arrived in its
     * parent folder.<br/>
     * The default is <code>false</code>.
     */
    public boolean isRecent() {
        return "true".equalsIgnoreCase(getProperty(PROPERTY_RECENT));
    }

    public void setUnread(boolean unread) {
        setProperty(PROPERTY_UNREAD, String.valueOf(unread));
    }

    /**
     * Returns <code>true</code> if the email is unread (incoming mail), or
     * if it has not been sent yet (outgoing mail).<br/>
     * The default is <code>true</code>.
     */
    public boolean isUnread() {
        return "true".equalsIgnoreCase(getProperty(PROPERTY_UNREAD));
    }

    public void setCreateTime(Date createTime) {
        setProperty(PROPERTY_CREATE_TIME, new MailDateFormat().format(createTime));
    }
    
    /**
     * Returns the date and time the email was submitted by the user, or <code>null</code>
     * if the value cannot be parsed.
     */
    public Date getCreateTime() {
        String dateStr = getProperty(PROPERTY_CREATE_TIME);
        Date createTime;
        try {
            createTime = new MailDateFormat().parse(dateStr);
        } catch (ParseException e) {
            log.error("Can't parse create time.", e);
            createTime = null;
        }
        return createTime;
    }
    
    public void setReplied(boolean replied) {
        setProperty(PROPERTY_REPLIED, replied ? "true" : "false");
    }
    
    public boolean isReplied() {
        String repliedStr = getProperty(PROPERTY_REPLIED);
        return "true".equals(repliedStr);
    }

    public void setDeleted(boolean deleted) {
        setProperty(PROPERTY_DELETED, deleted ? "true" : "false");
    }

    public boolean isDeleted() {
        String deletedStr = getProperty(PROPERTY_DELETED);
        return "true".equals(deletedStr);
    }
    
    /**
     * Adds metadata about an email packet that has been stored in the DHT
     * and is waiting to be picked up and deleted.
     * @param destination
     * @param dhtKey
     * @param delVerificationHash
     */
    public void addPacketInfo(EmailDestination destination, Hash dhtKey, Hash delVerificationHash) {
        int destIndex = getDestinationIndex(destination);
        int packetIndex = getPacketIndex(destIndex, dhtKey);
        setProperty(PROPERTY_DESTINATION + destIndex, destination.toBase64());
        String packetProperty = PROPERTY_DESTINATION + destIndex + "." + PACKET + packetIndex + ".";
        setProperty(packetProperty + DHT_KEY, dhtKey.toBase64());
        setProperty(packetProperty + DELETE_VERIFICATION_HASH, delVerificationHash.toBase64());
        setProperty(packetProperty + DELETED_FROM_DHT, "false");
    }
    
    private int getDestinationIndex(EmailDestination destination) {
        int destIndex = 0;
        while (true) {
            String value = getProperty(PROPERTY_DESTINATION + destIndex);
            if (value==null || destination.toBase64().equals(value))
                return destIndex;
            destIndex++;
        }
    }
    
    private int getPacketIndex(int destIndex, Hash dhtKey) {
        int pktIndex = 0;
        while (true) {
            String value = getProperty(PROPERTY_DESTINATION + destIndex + "." + PACKET + pktIndex + "." + DHT_KEY);
            if (value==null || dhtKey.toBase64().equals(value))
                return pktIndex;
            pktIndex++;
        }
    }
    
    public Hash getDeleteVerificationHash(EmailDestination destination, Hash dhtKey) {
        int destIndex = getDestinationIndex(destination);
        int packetIndex = getPacketIndex(destIndex, dhtKey);
        String hashStr = getProperty(PROPERTY_DESTINATION + destIndex + "." + PACKET + packetIndex + "." + DELETE_VERIFICATION_HASH);
        try {
            return Util.createHash(hashStr);
        }
        catch (DataFormatException e) {
            log.error("Invalid delete verification hash: <" + hashStr + ">", e);
            return null;
        }
    }

    public void setPacketDelivered(Hash dhtKey, boolean delivered) {
        String dhtKeyStr = dhtKey.toBase64();
        
        for (Object property: keySet())
            if (property instanceof String) {
                String propertyStr = (String)property;
                if (propertyStr.matches(PROPERTY_DESTINATION + ".*" + DHT_KEY) && dhtKeyStr.equals(getProperty(propertyStr))) {
                    String deletedProperty = propertyStr.replace(DHT_KEY, DELETED_FROM_DHT);
                    setProperty(deletedProperty, delivered ? "true" : "false");
                }
            }
    }
    
    public boolean isDelivered() {
        return getNumUndeliveredRecipients() == 0;
    }
    
    public int getDeliveryPercentage() {
        int numPackets = getNumPackets();
        if (numPackets == 0)
            return 0;
        else
            return 100 * getNumDeliveredPackets() / numPackets;
    }
    
    private int getNumDeliveredPackets() {
        int numUndelivered = 0;
        for (Object property: keySet())
            if (property instanceof String) {
                String delFlagProperty = (String)property;
                if (delFlagProperty.matches(PROPERTY_DESTINATION + ".*" + DELETED_FROM_DHT) && "true".equals(getProperty(delFlagProperty)))
                    numUndelivered++;
            }
        return numUndelivered;
    }
    
    /** Returns the number of email packets for the email. */
    private int getNumPackets() {
        int numPackets = 0;
        for (Object property: keySet())
            if (property instanceof String) {
                String delFlagProperty = (String)property;
                if (delFlagProperty.matches(PROPERTY_DESTINATION + ".*" + DELETED_FROM_DHT))
                    numPackets++;
            }
        return numPackets;
    }
    
    /** Returns the number of recipients who haven't picked up all email packets. */
    public int getNumUndeliveredRecipients() {
        int destIndex = 0;
        int numUndelivered = 0;
        while (true) {
            String value = getProperty(PROPERTY_DESTINATION + destIndex);
            if (value == null)
                break;
            if (!isDelivered(destIndex))
                numUndelivered++;
            destIndex++;
        }
        return numUndelivered;
    }
    
    /**
     * Tests if the email has been delivered to a given destination.
     * @param destIndex the destination index in the property key, e.g. 5 for "destination5"
     */
    private boolean isDelivered(int destIndex) {
        int packetIndex = 0;
        while (true) {
            String packetDeliveredStr = getProperty(PROPERTY_DESTINATION + destIndex + "." + PACKET + packetIndex + "." + DELETED_FROM_DHT);
            if (packetDeliveredStr == null)
                return true;
            boolean packetDelivered = "true".equalsIgnoreCase(packetDeliveredStr);
            if (!packetDelivered)
                return false;

            packetIndex++;
        }
    }
    
    /** Returns the DHT keys of all email packets which haven't been deleted from the DHT */
    public Collection<PacketInfo> getUndeliveredPacketKeys() {
        Collection<PacketInfo> packets = new ArrayList<PacketInfo>();
        for (Object property: keySet())
            if (property instanceof String) {
                String delFlagProperty = (String)property;
                if (delFlagProperty.matches(PROPERTY_DESTINATION + ".*" + DELETED_FROM_DHT) && "false".equals(getProperty(delFlagProperty))) {
                    String baseProperty = delFlagProperty.replace(DELETED_FROM_DHT, "");
                    String dhtKeyProperty = baseProperty + DHT_KEY;
                    String dhtKeyStr = getProperty(dhtKeyProperty);
                    String delVerifProperty = baseProperty + DELETE_VERIFICATION_HASH;
                    String delVerifStr = getProperty(delVerifProperty);
                    try {
                        Hash dhtKey = Util.createHash(dhtKeyStr);
                        Hash delVerifHash = Util.createHash(delVerifStr);
                        packets.add(new PacketInfo(dhtKey, delVerifHash));
                    }
                    catch (DataFormatException e) {
                        log.error("Invalid DHT key or verification hash in email metadata for property key " + baseProperty, e);
                    }
                }
            }
        
        return packets;
    }
    
    public void writeTo(OutputStream stream) throws IOException {
        store(stream, null);
    }
    
    /** Contains a DHT key and a verification hash for an email packet. */
    public class PacketInfo {
        public Hash dhtKey;
        public Hash delVerificationHash;
        
        private PacketInfo(Hash dhtKey, Hash delVerificationHash) {
            this.dhtKey = dhtKey;
            this.delVerificationHash = delVerificationHash;
        }
    }
}