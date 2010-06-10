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

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import i2p.bote.email.EmailDestination;
import i2p.bote.network.I2PPacketDispatcher;
import i2p.bote.network.I2PSendQueue;
import i2p.bote.service.RelayPeerManager;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import net.i2p.client.I2PClient;
import net.i2p.client.I2PClientFactory;
import net.i2p.client.I2PSession;
import net.i2p.client.I2PSessionException;
import net.i2p.data.Base64;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link RelayDataPacket} and {@link RelayRequest}.
 */
public class RelayPacketTest {
    private IndexPacket indexPacket;
    private RelayDataPacket relayDataPacket;
    private RelayRequest relayRequest;
    private String[] destKeys;
    private RelayPeerManager peerManager;
    private I2PClient i2pClient;

    @Before
    public void setUp() throws Exception {
        Destination nextDestination = new Destination("X3oKYQJ~1EAz7B1ZYGSrOTIMCW5Rnn2Svoc38dx5D9~zvz8vqiWcH-pCqQDwLgPWl9RTBzHtTmZcGRPXIv54i0XWeUfX6rTPDQGuZsnBMM0xrkH2FNLNFaJa0NgW3uKXWpNj9AI1AXUXzK-2MYTYoaZHx5SBoCaKfAGMcFJvTON1~kopxBxdBF9Q7T4~PJ3I2LeU-ycmUlehe9N9bIu7adUGyPGVl8Ka-UxwQromoJ~vSWHHl8HkwcDkW--v9Aj~wvFqxqriFkB1EeBiThi3V4XtVY~GUP4IkRj9YZGTsSBf3eS4xwXgnYWlB7IvxAGBfHY9MCg3lbAa1Dg~1IH6rhtXxsXUtGcXsz9yMZTxXHd~rGo~JrXeM1y~Vcenpr6tJcum6pxevkKzzT0qDegGPH3Zhqz7sSeeIaJEcPBUAkX89csqyFWFIjTMm6yZp2rW-QYUnVNLNTjf7vndYUAEICogAkq~btqpIzrGEpm3Pr9F23br3SpbOmdxQxg51AMmAAAA");
        
        EmailDestination destination = new EmailDestination("3LbBiN2nxtQVxPXYBQL3~PjBg-xOPalsFKZ0YqobHXP1u3MiBxqthF6TJxqdPS2LWWKb90FVzaPyIIEQOT0qSb");
        indexPacket = new IndexPacket(destination);
        relayRequest = new RelayRequest(indexPacket, nextDestination);
        
        long minDelayMilliseconds = TimeUnit.MILLISECONDS.convert(120, TimeUnit.MINUTES);
        long maxDelayMilliseconds = TimeUnit.MILLISECONDS.convert(600, TimeUnit.MINUTES);
        relayDataPacket = new RelayDataPacket(nextDestination, minDelayMilliseconds, maxDelayMilliseconds, relayRequest);
        
        // create a RelayPeerManager
        String localBase64DestKeys = "X3oKYQJ~1EAz7B1ZYGSrOTIMCW5Rnn2Svoc38dx5D9~zvz8vqiWcH-pCqQDwLgPWl9RTBzHtTmZcGRPXIv54i0XWeUfX6rTPDQGuZsnBMM0xrkH2FNLNFaJa0NgW3uKXWpNj9AI1AXUXzK-2MYTYoaZHx5SBoCaKfAGMcFJvTON1~kopxBxdBF9Q7T4~PJ3I2LeU-ycmUlehe9N9bIu7adUGyPGVl8Ka-UxwQromoJ~vSWHHl8HkwcDkW--v9Aj~wvFqxqriFkB1EeBiThi3V4XtVY~GUP4IkRj9YZGTsSBf3eS4xwXgnYWlB7IvxAGBfHY9MCg3lbAa1Dg~1IH6rhtXxsXUtGcXsz9yMZTxXHd~rGo~JrXeM1y~Vcenpr6tJcum6pxevkKzzT0qDegGPH3Zhqz7sSeeIaJEcPBUAkX89csqyFWFIjTMm6yZp2rW-QYUnVNLNTjf7vndYUAEICogAkq~btqpIzrGEpm3Pr9F23br3SpbOmdxQxg51AMmAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADBrMolZp2gbXxrAef~UFJdSfiKSSSj~KcUOKndCndDipKboKQ5rcwHu0eKElE5NIS";
        i2pClient = I2PClientFactory.createClient();
        I2PSession i2pSession = i2pClient.createSession(new ByteArrayInputStream(Base64.decode(localBase64DestKeys)), null);
        I2PPacketDispatcher i2pReceiver = new I2PPacketDispatcher();
        I2PSendQueue sendQueue = new I2PSendQueue(i2pSession, i2pReceiver);
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File testDir = new File(tempDir, "RelayPacketTest-" + System.currentTimeMillis());
        if (!testDir.mkdirs())
            fail("Cannot create directory: <" + testDir + ">");
        destKeys = new String[3];
        destKeys[0] = "EN2qXoeZ-x3qCejofePmMfW~1gWo4~kKyRQg5NwzGW-gZ5rKD1pUN8eHFfEid6v7GLl6XRTOCPw4OELnqKhhtLSaQQz3U9qTcpbmS1Oc9U2cpqwiOwyGru-5RF8LU9s7VwhUTB1nh~NrSzEJlbCZ9eOZiJyw7t031aHRbWTIOniq3vUkEVpEkhYbO8TlNT~4KzvINoDGUsEr2b6vV6j8IpZN-J8Jd9mRBJCaY1dxyMJ~2rZ441m2q3ndHbu5~GUPBelf7nnTh2ggzdgtHJuy~j2MOJda65rPxBEZq8cOmN-YyA5-yagIPL34QH6yp10bERN8S3qz2LeyVRcvzDEhsJUOVYv2Bt67gJ-KhXyCB7qSpQjRaT~QKpYoKfdCH2eqYUA9Yj1QaZJZjWQTN6gGf0DUHNNz2VEBRyzkYNd7XHgt5vCIhSw0U2N6s~62iEzTQHF8QbgKYeUTmx8XVOgvSau0tdg-ZzG0-UKJ3Zzng93E75B3W7juUGA~Z59fXF-PAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADRgSFr5W3QTfIjiKy1UjULHdz0nDnVQaC4OQOJ0baPyAq6IWu3cLrkTF5uXL9yk3r";
        destKeys[1] = "0hyT9FyR6yGcum~KP5jw40AGz-rhYvsNQd4CKrJooZtmy211zietpD8YukY3yJQ7w2XRel9CgnbkwuYd-LmdwMbcqgpn8RYwJNPU7luJuZcxNUvMsJ-TVjmVRY-6-Q~HrKABC2DU-5iQU2Qscvi3YqRiwerdK2Dd4PpPsmWZmCByUkLG46ELwjhR12JjflbPqwKPyd0EN-cDxNxj73hGMacxOw3awATkX8pBzIXvGZ6vjAsktIzNODJMLTizuILpdj-7jW8~MeRwEJGFFhHopZZkSQsAKDX5eM~gWXP9-cK3TSw-Yjx6AMZZ082SKhESW09Ec9Nj-U0tCQpgAhAI02GFS~knlBbELZGnBXS42hrMo6MDy71WhLvhqKV-Jt62AOX2MGyAy6FBAva3HEFDFVVBEkuaCF7QIGYwiy-BnQjC4GwivpcxxsAwgRE9h1tcuJozc52Cmp7nRGbPIHaCA0fEFzHh1~ObhpAqUEuWkmdccaVeDdxLkZrb4QQe~rrWAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB6GbqdiiIgugHmtMH0qypF1etm04sGM7X5sXVSaTG-4f0zGPSrvhsRCAT7y7gP0ae";
        destKeys[2] = "ypxYUiP70IurS7HBRaFUIl5UxAwIo0pboudvT9FrY~JcFTOVYxd3u2K9QW9IAvyeceQgXMKZVNmL6fdw8j3DDSS8faB043MMM2RB6md9ziRwBW60r7tt0b9-Jn-~98lBv4ilwH8H0ivd8S7xEsbFT0W7dvqyInJ0kkC8yfX5bUlQYedkozQxl8LikSZWL4hyfGkNcHmLGl11OkRdBf-QjIEEdUE614iVY5bRxc~XS9TedXuRFfSciwVB14M7RRz0-mY61uNeH-7tVJZ1ixsoHBCoXqY9TFZ33zZkjyUQaFJaJxWVA5069ureV0BpvO-wBfCdTqUe92v5-hZaCqzlr3PqWLMAhR82yUZlarlZdeeTvnU-4g1~2tsnyUZQlev2i9jmpCb2~N0goo6hPOWTfWADsgq8xABIfi7mUchlMz~tuzCZwOUx2WVjy66u-tccHEdGC737zyPEhvOmZqjo3tg54IeX72FVP7kHc2QYNbfO7HXzK1x6FQ0dTr-E-OUeAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADFD91On9qR24Vcrdp-QmybkllCrh6k~3FcBUcIETv1AG79DSell4tLImTenGkRKt1";
        File peerFile = new File(testDir, "peers.txt");
        BufferedWriter writer = new BufferedWriter(new FileWriter(peerFile));
        writer.write(destKeys[0]);
        writer.newLine();
        writer.write(destKeys[1]);
        writer.newLine();
        writer.write(destKeys[2]);
        writer.newLine();
        writer.close();
        peerManager = new RelayPeerManager(sendQueue, i2pSession.getMyDestination(), peerFile);
        assertEquals(destKeys.length, peerManager.getAllPeers().size());
    }

    @After
    public void tearDown() throws Exception {
    }
    
    @Test
    public void toByteArrayAndBack() throws DataFormatException, MalformedDataPacketException {
        byte[] bytes, bytes2;
        
        // test RelayRequest
        bytes = relayRequest.toByteArray();
        bytes2 = new RelayRequest(bytes).toByteArray();
        assertTrue("The two packets differ!", Arrays.equals(bytes, bytes2));

        // test RelayDataPacket
        bytes = relayDataPacket.toByteArray();
        bytes2 = new RelayDataPacket(bytes).toByteArray();
        assertTrue("The two packets differ!", Arrays.equals(bytes, bytes2));
    }
    
    @Test
    public void testCreateAndUnwrap() throws I2PSessionException, DataFormatException, NoSuchAlgorithmException, MalformedDataPacketException {
        long minDelayMilliseconds = TimeUnit.MILLISECONDS.convert(120, TimeUnit.MINUTES);
        long maxDelayMilliseconds = TimeUnit.MILLISECONDS.convert(600, TimeUnit.MINUTES);
        DataPacket dataPacket = RelayDataPacket.create(indexPacket, peerManager, destKeys.length, minDelayMilliseconds, maxDelayMilliseconds);
        byte[] indexPacketBytes = indexPacket.toByteArray();
        Assert.assertNotNull(dataPacket);
        
        for (int i=0; i<destKeys.length; i++) {
            assertTrue(dataPacket instanceof RelayDataPacket);
            RelayDataPacket relayDataPacket = (RelayDataPacket)dataPacket;
            RelayRequest request = relayDataPacket.getRequest();
            dataPacket = decryptDataPacket(request, relayDataPacket.getNextDestination());
        }
        assertTrue(Arrays.equals(indexPacketBytes, dataPacket.toByteArray()));
    }
    
    private DataPacket decryptDataPacket(RelayRequest request, Destination destination) throws I2PSessionException, DataFormatException, NoSuchAlgorithmException, MalformedDataPacketException {
        for (String destKey: destKeys)
            if (destKey.startsWith(destination.toBase64())) {
                I2PSession i2pSession = i2pClient.createSession(new ByteArrayInputStream(Base64.decode(destKey)), null);
                return request.getStoredPacket(i2pSession);
            }
        fail("No matching I2PSession for I2P destination in packet: <" + destination.toBase64() + ">");
        return null;
    }
}