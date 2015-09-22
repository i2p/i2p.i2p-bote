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

import static i2p.bote.Util._t;
import i2p.bote.I2PBote;
import i2p.bote.Util;
import i2p.bote.network.DhtPeerStats;

import java.math.BigInteger;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.i2p.data.Hash;

/**
 * Stores information on Kademlia peers.
 * @see DhtPeerStats
 */
class KademliaPeerStats implements DhtPeerStats {
    private List<String> header;
    private List<List<String>> data;
    
    KademliaPeerStats(SBucket sBucket, List<KBucket> kBuckets, Hash localDestinationHash) {
        String[] headerArray = new String[] {_t("Peer"), _t("I2P Destination"), _t("BktPfx"), _t("Distance"), _t("Locked?"), _t("First Seen")};
        header = Arrays.asList(headerArray);
        
        data = new ArrayList<List<String>>();
        addPeerData(sBucket, localDestinationHash);
        for (KBucket kBucket: kBuckets)
            addPeerData(kBucket, localDestinationHash);
    }
    
    private void addPeerData(AbstractBucket bucket, Hash localDestinationHash) {
        Locale locale = new Locale(I2PBote.getLanguage());
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
        
        for (KademliaPeer peer: bucket) {
            List<String> row = new ArrayList<String>();
            row.add(String.valueOf(data.size() + 1));
            row.add(Util.toBase32(peer));
            row.add(getBucketPrefix(bucket));
            BigInteger distance = KademliaUtil.getDistance(localDestinationHash, peer.calculateHash());
            row.add(distance.shiftRight((Hash.HASH_LENGTH-2)*8).toString());   // show the 2 most significant bytes
            row.add(String.valueOf(peer.isLocked() ? _t("Yes")+"("+(peer.getConsecTimeouts())+")" : _t("No")));
            String firstSeen = formatter.format(peer.getFirstSeen());
            row.add(String.valueOf(firstSeen));
            data.add(row);
        }
    }

    /**
     * Returns the common prefix shared by the binary representation of all peers in the bucket.
     * @param bucket
     * @return
     */
    private String getBucketPrefix(AbstractBucket bucket) {
        if (bucket instanceof KBucket) {
            KBucket kBucket = (KBucket)bucket;
            String prefix = kBucket.getBucketPrefix();
            if (prefix.isEmpty())
                return _t("(None)");
            else
                return prefix;
        }
        else
            return _t("(S)");
    }
    
    @Override
    public List<String> getHeader() {
        return header;
    }

    @Override
    public List<List<String>> getData() {
        return data;
    }
}
