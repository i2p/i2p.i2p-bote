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

public class KademliaConstants {
    /** Number of redundant storage nodes */
    public static final int K = 20;
    
    /** The size of the sibling list for S/Kademlia */
    public static final int S = 100;
    
//    public static final int B = 5;   // This is the value from the original Kademlia paper.
    public static final int B = 1;

    /** According to the literature, 3 is the optimum choice, but until the network becomes significantly larger than S, we'll use a higher value for speed. */
    public static final int ALPHA = 10;
    
    /** The amount of time after which a bucket is refreshed if a lookup hasn't been done in its ID range */
    public static final int BUCKET_REFRESH_INTERVAL = 3600 * 1000;
    
    /** Time interval for Kademlia replication (plus or minus <code>REPLICATE_VARIANCE</code>) */
    public static final int REPLICATE_INTERVAL = 3600 * 1000;
    
    /** the maximum amount of time the replication interval can deviate from REPLICATE_INTERVAL */
    public static final long REPLICATE_VARIANCE = 5 * 60 * 1000;

    private KademliaConstants() { }
}