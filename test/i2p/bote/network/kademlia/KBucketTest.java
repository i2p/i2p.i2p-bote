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

import static org.junit.Assert.*;

import java.math.BigInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class KBucketTest {
/*    private static final BigInteger START_ID = new BigInteger();
    private static final BigInteger END_ID = new BigInteger();

    private List<Destination> destinations;

    @Before
    public void setUp() throws Exception {
        KBucket bucket = new KBucket(START_ID, END_ID, KademliaConstants.K, 0);
        destinations = ArrayList<Destination>();
        for (int i=0; i<KademliaConstants.K; i++)
            destinations.add(new Destination(random_dest));
    }

    @Test
    public void testAdd() {
        Destination dest = destinations.get(0);
        * add dest
        * verify that n=1 and get(0) equals dest
        * add dests 0..2
        * verify that n=3 and get(i) equals destinations.get(i) for i=0..2
        
        fail("Not yet implemented");
    }

    @Test
    public void testAddOrSplit() {
        assertEqusals("K must be an even number for this test to work.", 0, KademliaConstants.K%2);

        for (i=0; i<KademliaConstants.K, i++) {
            KBucket newBucket = bucket.addOrSplit(destinations.get(i));
            assertNull(newBucket);
        }
        KBucket newBucket = bucket.addOrSplit(destinations.get(KademliaConstants.K));
        assertNotNull(newBucket);
        assertEquals(KademliaConstants.K/2, bucket.size());
        assertEquals(1, bucket.getDepth());
        assertEquals(1, newBucket.getDepth());
    }

    @Test
    public void testAddOrUpdate() {
        * add K, should work
        * add another one, should throw an exc
    }

    @Test
    public void testRemove() {
        assertEquals(0, bucket.size());
        * add 1
        assertEquals(1, bucket.size());
        * remove it
        assertEquals(0, bucket.size());
        * add 3
        assertEquals(3, bucket.size());
        * remove 2
        assertEquals(1, bucket.size());
        * remove the remaining 1
        assertEquals(0, bucket.size());
    }

    @Test
    public void testIsFull() {
        assertEquals(false, bucket.isFull());
        * add 1
        assertEquals(false, bucket.isFull());
        * add another K-2
        assertEquals(false, bucket.isFull());
        * add one more
        assertEquals(true, bucket.isFull());
    }
    
    TODO test replacement cache*/
}