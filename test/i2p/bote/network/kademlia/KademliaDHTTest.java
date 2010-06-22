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

package i2p.bote.network.kademlia;

import static org.junit.Assert.fail;
import i2p.bote.network.I2PPacketDispatcher;
import i2p.bote.network.I2PSendQueue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import net.i2p.client.I2PClient;
import net.i2p.client.I2PClientFactory;
import net.i2p.client.I2PSession;
import net.i2p.data.Destination;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class KademliaDHTTest {
    private static final int NUM_NODES = 100;
    
    private Collection<KademliaDHT> nodes;

    @Before
    public void setUp() throws Exception {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File testDir = new File(tmpDir, "I2PBote-Test_" + System.currentTimeMillis());
        testDir.mkdir();
        
        I2PClient i2pClient = I2PClientFactory.createClient();
        
        Destination firstNode = null;
        
        nodes = Collections.synchronizedList(new ArrayList<KademliaDHT>());
        for (int i=0; i<NUM_NODES; i++) {
            ByteArrayOutputStream arrayStream = new ByteArrayOutputStream();
            Destination destination = i2pClient.createDestination(arrayStream);
            byte[] destinationArray = arrayStream.toByteArray();
            I2PSession i2pSession = i2pClient.createSession(new ByteArrayInputStream(destinationArray), null);
            
            I2PPacketDispatcher packetDispatcher = new I2PPacketDispatcher();
            i2pSession.addSessionListener(packetDispatcher, I2PSession.PROTO_DATAGRAM, I2PSession.PORT_ANY);
            
            I2PSendQueue sendQueue = new I2PSendQueue(i2pSession, packetDispatcher);
            
            File peerFile = new File(testDir, "peers" + i);
            if (firstNode != null) {
                FileWriter writer = new FileWriter(peerFile);
                writer.write(firstNode.toBase64());
            }
            else
                firstNode = destination;
            
            nodes.add(new KademliaDHT(sendQueue, packetDispatcher, peerFile));
        }
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testBootstrap() {
        fail("Not yet implemented");
    }
    
    @Test
    public void testFindOne() {
        fail("Not yet implemented");
    }

    @Test
    public void testFindAll() {
        fail("Not yet implemented");
    }

    @Test
    public void testStore() {
        fail("Not yet implemented");
    }
}