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
    public static final int K = 2;   // Number of redundant storage nodes.
    public static final int S = 3;   // The size of the sibling list for S/Kademlia.
//    public static final int B = 5;   // This is the value from the original Kademlia paper.
    public static final int B = 1;
    public static final int ALPHA = 3;   // According to the literature, this is the optimum choice for alpha.
    public static final int REFRESH_TIMEOUT = 3600;
    public static final int REPLICATE_INTERVAL = 3600;   // TODO would it be better for REPLICATE_INTERVAL to be slightly longer than REFRESH_TIMEOUT?
    public static final int STALE_THRESHOLD = 3;   // Maximum number of times a peer can time out in a row before it is dropped

    private KademliaConstants() { }
}