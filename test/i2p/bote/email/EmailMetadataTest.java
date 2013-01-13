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

import static org.junit.Assert.assertEquals;

import java.security.GeneralSecurityException;

import net.i2p.data.Hash;

import org.junit.Test;

public class EmailMetadataTest {

    /** Tests addPacketPendingDelivery(), getDeleteVerificationHash(), and getNumUndeliveredRecipients() */
    @Test
    public void testPacketTracking() throws GeneralSecurityException {
        EmailDestination destination1 = new EmailDestination("3LbBiN2nxtQVxPXYBQL3~PjBg-xOPalsFKZ0YqobHXP1u3MiBxqthF6TJxqdPS2LWWKb90FVzaPyIIEQOT0qSb");
        EmailDestination destination2 = new EmailDestination("m-5~1dZ0MrGdyAWu-C2ecNAB5LCCsHQpeSfjn-r~mqMfNvroR98~BRmReUDmb0la-r-pBHLMtflrJE7aTrGwDTBm5~AJFEm-9SJPZnyGs-ed5pOj4Db65yJml1y1n77qr1~mM4GITl6KuIoxg8YwvPrCIlXe2hiiDCoC-uY9-np9UY");
        Hash dhtKey1 = new Hash(new byte[] {-37, -8, 37, 82, -40, -34, 68, -51, -16, 74, 27, 89, 113, -15, 112, 69, 92, 102, 62, 111, 99, -27, -42, -71, 6, 38, 106, 121, 21, -72, -83, 3});
        Hash dhtKey2 = new Hash(new byte[] {-69, -24, -109, 1, 69, -122, -69, 113, -68, -90, 55, -28, 105, 97, 125, 70, 51, 58, 14, 2, -13, -53, 90, -29, 36, 67, 36, -94, -108, -125, 11, 123});
        Hash dhtKey3 = new Hash(new byte[] {120, 120, -8, -88, 21, 126, 46, -61, 18, -101, 15, 53, 20, -44, -112, 42, 86, -117, 30, -96, -66, 33, 71, -55, -102, -78, 78, -82, -105, 66, -116, 43});
        Hash dhtKey4 = new Hash(new byte[] {-62, -112, 99, -65, 13, 44, -117, -111, 96, 45, -6, 64, 78, 57, 117, 103, -24, 101, 106, -116, -18, 62, 99, -49, 60, -81, 8, 64, 27, -41, -104, 58});
        Hash delVerificationHash1 = new Hash(new byte[] {-48, 78, 66, 58, -79, 87, 38, -103, -60, -27, 108, 55, 117, 37, -99, 93, -23, -102, -83, 20, 44, -80, 65, 89, -68, -73, 69, 51, 115, 79, 24, 127});
        Hash delVerificationHash2 = new Hash(new byte[] {6, -32, -23, 17, 55, 15, -45, -19, 91, 100, -76, -76, 118, -118, -53, -109, -108, 113, -112, 81, 117, 9, -126, 20, 0, -83, -89, 7, 48, 76, -58, 83});
        Hash delVerificationHash3 = new Hash(new byte[] {-16, 67, 107, 80, 27, 65, 81, 71, 61, 70, -72, 126, 64, -10, 57, -128, 111, -107, -42, 24, -90, -4, -46, 63, 7, -6, 43, 76, -9, -81, 8, -68});
        Hash delVerificationHash4 = new Hash(new byte[] {-69, -24, -109, 1, 69, -122, -69, 113, -68, -90, 55, -28, 105, 97, 125, 70, 51, 58, 14, 2, -13, -53, 90, -29, 36, 67, 36, -94, -108, -125, 11, 123});
        
        EmailMetadata metadata = new EmailMetadata();
        
        assertEquals(null, metadata.getDeleteVerificationHash(destination1, dhtKey1));
        assertEquals(null, metadata.getDeleteVerificationHash(destination1, dhtKey2));
        assertEquals(null, metadata.getDeleteVerificationHash(destination1, dhtKey3));
        assertEquals(null, metadata.getDeleteVerificationHash(destination2, dhtKey4));

        metadata.addPacketInfo(destination1, dhtKey1, delVerificationHash1);
        metadata.addPacketInfo(destination1, dhtKey2, delVerificationHash2);
        metadata.addPacketInfo(destination1, dhtKey3, delVerificationHash3);
        metadata.addPacketInfo(destination2, dhtKey4, delVerificationHash4);
        
        assertEquals(2, metadata.getNumUndeliveredRecipients());
        assertEquals(delVerificationHash1, metadata.getDeleteVerificationHash(destination1, dhtKey1));
        assertEquals(delVerificationHash2, metadata.getDeleteVerificationHash(destination1, dhtKey2));
        assertEquals(delVerificationHash3, metadata.getDeleteVerificationHash(destination1, dhtKey3));
        assertEquals(delVerificationHash4, metadata.getDeleteVerificationHash(destination2, dhtKey4));
        assertEquals(null, metadata.getDeleteVerificationHash(destination1, dhtKey4));
        assertEquals(null, metadata.getDeleteVerificationHash(destination2, dhtKey3));
        
        assertEquals(2, metadata.getNumUndeliveredRecipients());
        metadata.setPacketDelivered(dhtKey1, true);
        assertEquals(2, metadata.getNumUndeliveredRecipients());
        metadata.setPacketDelivered(dhtKey2, true);
        assertEquals(2, metadata.getNumUndeliveredRecipients());
        metadata.setPacketDelivered(dhtKey3, true);
        assertEquals(1, metadata.getNumUndeliveredRecipients());
        metadata.setPacketDelivered(dhtKey4, true);
        assertEquals(0, metadata.getNumUndeliveredRecipients());
    }
}