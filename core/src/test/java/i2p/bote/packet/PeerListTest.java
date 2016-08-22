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

import static org.junit.Assert.assertArrayEquals;
import java.util.ArrayList;
import java.util.Collection;

import net.i2p.data.Destination;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PeerListTest {
    PeerList peerList;

    @Before
    public void setUp() throws Exception {
        Collection<Destination> peers = new ArrayList<Destination>();
        peers.add(new Destination("PX~Oa14i79JumDyAntlanQS1tecQVBNBmZm7g31pcaOl03JPGqTaZYXYn2zVPsAURiuhSltfbEj7sIqcGhbv2KWeRJjqc-4wEHix41qGo2Unzgo~6wkHTv72wwtWCUSNZ~Tbx1XUxsf8m2LVYm~G8xy-OkK2YJ9rrRTnfNn7oSOwvngtMbDXRriJuEBnccHhYw0UoqUzcWtTzml7xlwZyNg~O5vWEOeifeMq~8fcCbqSTCAkE3eMzdNPCehMUgDgu4cVPmy-28vqp6zYcg1jSoITi265sw9tZcY6LtPhENc3qyh-cR0VI~1R4tGJ-m6xio8MzyuMJEhFay8U1C4NhxnV4KN37VR2go-KKfM83XSf6w0vemFu0hmUQ0fzKeRw95Gmn-U~EJ2zMXIrxb3ElguGJNRXodah9t4AgBqzsOXIUv4yDouhw5hPVlhG0biTvU9ozHz6ZBDPrblvieGWhYcNA79pma3QmIyfeNuKGxgq7Hy49iXbFiTddG88qcGjAAAA"));
        peers.add(new Destination("sXzHjwSSwzSRBxXnLyUHpv7lxAEtI2iF7spBvNwlLZT~Q1GPsCC4I6y9pWgdrhOlZ04PWT--yuEMcLED-72bzlfl-jHTbqa2He1Qc2bAI4irkpiVC1BsHgGhANr2wt8Zhayepb-izxV4MRdZV0v6sNu9lwlMSN0-Z9YLsPirb-G3zkzO2jBKZv7MMOyKtNRoJY0aXnj~SnvcECj1nc5MmMvFWsyV0o3EjB6zSUAl1pI9Z7ecBK2GKHZqZOVXhAGhx8imp-NUyVJK3g7h0Kf~3g457TxBt4ccbnNBvbWyMVbgEqjkC9P3uN6jezaYkFvBQ2xuNV~kXYM1vJF8Ao~efSBpbuebyUZQyJiB2NNK4d60JjGWlao8S7ISbpVypjdG~x5vvaCx0gl30uecLTmlDLQRR0e56JkyjWDvfNo0cAbDjTUpZq9UK9ZtQCmyhxm-Ob7Os0qnvOx1X2Q6FAWep04Kk~bGAkLwlmfl~4F5E2JvwOuNU7XqJdh~L3LI2K~pAAAA"));
        peers.add(new Destination("xiOuq3foxfzt7r570xxMo97OKpxsALO2g1qXcVxAWgp0IOrikk9lNjB-wtSARjTHB3rqHtsbt-RDEwV7j-AQ-EIh-X3ZtmeHlJw2bQzN~mhwSwr2hgOdC8j0CviMZab9U-9ZtxZrSISY6Pqkjst~sydY~3zzGdeatpcK2sA04bzrVgvLuv3soYiu-eCCa7CZne9Mn8os94LdaUBrNs-rQLxGOo-35FN8xyqfDRNzsvPZstL7jW1IOF14X-iaxB8lB8uIGHtrtoWBuYPoq7S~Db~Lo1ePmvq9x2qG5jQdvZ5-RdguIjDaIeGmV1D-lvLQ8s7xvpXIFqwX~7i7IVbRSk-NsGZiu0M8k15zic~rV8Z-xG8JnUSXEhv2ClxOf~512gAYLLknW8qUBAXhDxxT4nXMfQOR8a4Tc23aK3DioTZfENbCuoDnSUt0KyryrvFq8kMhJTWQ511IPXytP7Q~MXP1cqByGJXWKbEy-ETZnbHqL8NYgiqP1HNfApLH37gsAAAA"));
        peerList = new PeerList(peers);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void toByteArrayAndBack() throws Exception {
        byte[] arrayA = peerList.toByteArray();
        byte[] arrayB;
        arrayB = new PeerList(arrayA).toByteArray();
        assertArrayEquals("The two arrays differ!", arrayA, arrayB);
    }
}