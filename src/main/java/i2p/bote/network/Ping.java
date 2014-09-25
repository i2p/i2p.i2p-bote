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

package i2p.bote.network;

import i2p.bote.Util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.i2p.I2PAppContext;
import net.i2p.client.I2PClient;
import net.i2p.client.I2PClientFactory;
import net.i2p.client.I2PSession;
import net.i2p.client.I2PSessionException;
import net.i2p.client.I2PSessionListener;
import net.i2p.data.Destination;

public class Ping {
    private static final int DEFAULT_NUM_BYTES = 10000;
    private static final int DEFAULT_NUM_PINGS = 10;
    private static final int DEFAULT_INTERVAL = 1;   // in seconds
    
    private I2PSession i2pSession;
    
    private Ping() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    i2pSession.destroySession();
                    System.out.println("Finished");
                } catch (I2PSessionException e) {
                    e.printStackTrace();
                }
            }
         });
         
        // initialize i2pSession
        I2PClient i2pClient = I2PClientFactory.createClient();
        ByteArrayOutputStream arrayStream = new ByteArrayOutputStream();
        i2pClient.createDestination(arrayStream);
        byte[] destinationArray = arrayStream.toByteArray();
        i2pSession = i2pClient.createSession(new ByteArrayInputStream(destinationArray), null);
        i2pSession.connect();
    }
    
    private void listen() throws Exception {
        System.out.println("Listening for pings on I2P destination " + Util.toBase32(i2pSession.getMyDestination()) + ".b32.i2p");
        i2pSession.addSessionListener(new I2PSessionListener() {
            private int count;
            
            @Override
            public void reportAbuse(I2PSession session, int severity) { }
            
            @Override
            public void messageAvailable(I2PSession session, int msgId, long size) {
                try {
                    count++;
                    
                    byte[] payload = session.receiveMessage(msgId);
                    String sender = new String(Arrays.copyOf(payload, 60));
                    System.out.println("#" + count + " " + new Date() + " sender=" + sender + " length=" + payload.length + " bytes");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            @Override
            public void errorOccurred(I2PSession session, String message, Throwable error) {
                error.printStackTrace();
            }
            
            @Override
            public void disconnected(I2PSession session) {
                System.err.println("Disconnected!");
            }
        }, I2PSession.PROTO_DATAGRAM, I2PSession.PORT_ANY);
        
        new CountDownLatch(1).await();   // wait forever
    }
    
    private void doPing(String b32Destination, int numBytes, int numPings, int interval) throws Exception {
        if (!b32Destination.endsWith(".b32.i2p"))
            b32Destination += ".b32.i2p";
        Destination destination = I2PAppContext.getGlobalContext().namingService().lookup(b32Destination);
        
        // send our b32 destination followed by random data
        byte[] b32Bytes = b32Destination.getBytes();
        byte[] padBytes = new byte[numBytes - b32Bytes.length];
        Random random = new Random();
        random.nextBytes(padBytes);
        
        // concatenate the two arrays
        byte[] datagram = Arrays.copyOf(b32Bytes, numBytes);
        System.arraycopy(padBytes, 0, datagram, b32Bytes.length, padBytes.length);
        
        System.out.println("Pinging " + b32Destination + " " + numPings + " times with " + numBytes + " bytes at " + interval + "-second intervals");
        for (int i=0; i<numPings; i++) {
            if (i > 0)
                TimeUnit.SECONDS.sleep(interval);
            i2pSession.sendMessage(destination, datagram, I2PSession.PROTO_DATAGRAM, I2PSession.PORT_UNSPECIFIED, I2PSession.PORT_UNSPECIFIED);
            System.out.print(".");
        }
        System.out.println();
    }
    
    public static void printUsage() {
        System.out.println("This is a datagram test program. It can send or receive datagrams.");
        System.out.println();
        System.out.println("Syntax:");
        System.out.println("  Ping listen");
        System.out.println("Or:");
        System.out.println(" Ping <b32> [numBytes] [numPings] [interval]");
        System.out.println();
        System.out.println("When invoked with the parameter \"listen\", it generates an I2P destination, prints the .b32 address,");
        System.out.println("and receives datagrams until Ctrl-C is pressed.");
        System.out.println("In the second mode, it sends datagrams to a b32 destination. By default, " + DEFAULT_NUM_PINGS + " datagrams of " + DEFAULT_NUM_BYTES + " bytes are sent at one-second intervals");
    }
    
    public static void main(String[] args) throws Exception {
        Ping ping = new Ping();
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }
        else if ("listen".equalsIgnoreCase(args[0]))
            ping.listen();
        else {
            int numBytes = args.length>=2 ? Integer.valueOf(args[1]) : DEFAULT_NUM_BYTES;
            int numPings = args.length>=3 ? Integer.valueOf(args[2]) : DEFAULT_NUM_PINGS;
            int interval = args.length>=4 ? Integer.valueOf(args[3]) : DEFAULT_INTERVAL;
            ping.doPing(args[0], numBytes, numPings, interval);
        }
    }
}