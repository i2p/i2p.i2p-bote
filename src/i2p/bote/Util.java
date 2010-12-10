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

package i2p.bote;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import net.i2p.I2PAppContext;
import net.i2p.data.Base32;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.data.PrivateKey;
import net.i2p.data.PublicKey;
import net.i2p.data.SessionKey;
import net.i2p.util.Log;
import net.i2p.util.Translate;

public class Util {
    private static final String BUNDLE_NAME = "i2p.bote.locale.Messages";
    
    private Util() { }
    
    public static byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[32*1024];
        while (true) {
            int bytesToRead = Math.min(inputStream.available(), buffer.length);
            if (bytesToRead <= 0)
                break;
            else {
                int bytesRead = inputStream.read(buffer, 0, bytesToRead);
                byteStream.write(buffer, 0, bytesRead);
            }
        }
        return byteStream.toByteArray();
    }

    /**
     * Opens a <code>URL</code> and reads one line at a time.
     * Returns the lines as a <code>List</code> of <code>String</code>s,
     * or an empty <code>List</code> if an error occurred.
     * @param url
     * @see #readLines(File)
     */
    public static List<String> readLines(URL url) {
        Log log = new Log(Util.class);
        log.info("Reading URL: <" + url + ">");
        
        InputStream stream = null;
        try {
            stream = url.openStream();
            return readLines(stream);
        }
        catch (IOException e) {
            log.error("Error reading URL.", e);
            return Collections.emptyList();
        } finally {
            if (stream != null)
                try {
                    stream.close();
                } catch (IOException e) {
                    log.error("Can't close input stream.", e);
                }
        }
    }
    
    /**
     * Opens a <code>File</code> and reads one line at a time.
     * Returns the lines as a <code>List</code> of <code>String</code>s,
     * or an empty <code>List</code> if an error occurred.
     * @param file
     * @see #readLines(URL)
     */
    public static List<String> readLines(File file) {
        Log log = new Log(Util.class);
        log.info("Reading file: <" + file.getAbsolutePath() + ">");
        
        InputStream stream = null;
        try {
            stream = new FileInputStream(file);
            return readLines(stream);
        } catch (IOException e) {
            log.error("Error reading file.", e);
            return Collections.emptyList();
        } finally {
            if (stream != null)
                try {
                    stream.close();
                } catch (IOException e) {
                    log.error("Can't close input stream.", e);
                }
        }
    }
    
    /**
     * Opens an <code>InputStream</code> and reads one line at a time.
     * Returns the lines as a <code>List</code> of <code>String</code>s.
     * or an empty <code>List</code> if an error occurred.
     * @param url
     * @return
     * @see readLines(URL)
     */
    private static List<String> readLines(InputStream inputStream) throws IOException {
        Log log = new Log(Util.class);
        
        BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(inputStream));
        List<String> lines = new ArrayList<String>();
        
        while (true) {
            String line = null;
            line = inputBuffer.readLine();
            if (line == null)
                break;
            lines.add(line);
        }
        
        log.info(lines.size() + " lines read.");
        return lines;
    }
    
    /**
     * Reads all data from an input stream and writes it to an output stream.
     * @param input
     * @param output
     * @throws IOException
     */
    public static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024*1024];
        int bytesRead;
        while ((bytesRead=input.read(buffer)) >= 0)
            output.write(buffer, 0, bytesRead);
    }
    
    /**
     * Creates an I2P destination with a null certificate from 384 bytes that
     * are read from a <code>ByteBuffer</code>.
     * @param buffer
     * @throws DataFormatException 
     */
    public static Destination createDestination(ByteBuffer buffer) throws DataFormatException {
        byte[] bytes = new byte[388];
        // read 384 bytes, leave the last 3 bytes zero
        buffer.get(bytes, 0, 384);
        Destination peer = new Destination();
        peer.readBytes(bytes, 0);
        return peer;
    }
    
    /** Returns a <code>ThreadFactory</code> that creates threads that run at minimum priority */
    public static ThreadFactory createThreadFactory(final String threadName, final int stackSize) {
        return createThreadFactory(threadName, stackSize, Thread.MIN_PRIORITY);
    }
    
    public static ThreadFactory createThreadFactory(final String threadName, final int stackSize, final int priority) {
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread newThread = new Thread(Thread.currentThread().getThreadGroup(), runnable, threadName, stackSize);
                newThread.setPriority(priority);
                return newThread;
            }
        };
    }
    
    /**
     * Creates a thread-safe <code>Iterable</code> from a thread-unsafe one.
     * Modifications to the old <code>Iterable</code> will not affect the
     * new one.
     * @param <E>
     * @param iterable
     */
    public static <E> Iterable<E> synchronizedCopy(Iterable<E> iterable) {
        synchronized(iterable) {
            Collection<E> collection = new ArrayList<E>();
            for (E element: iterable)
                collection.add(element);
            return collection;
        }
    }

    /**
     * Returns the <code>i</code>-th element of a <code>Collection</code>'s <code>Iterator</code>.
     * @param <E>
     * @param collection
     * @param i
     */
    public static <E> E get(Collection<E> collection, int i) {
        for (E element: collection) {
            if (i == 0)
                return element;
            i--;
        }
        return null;
    }
    
    public static String _(String messageKey) {
        return Translate.getString(messageKey, I2PAppContext.getGlobalContext(), BUNDLE_NAME);
    }
    
    public static String _(String messageKey, Object parameter) {
        return Translate.getString(messageKey, parameter, I2PAppContext.getGlobalContext(), BUNDLE_NAME);
    }
    
    public static String _(String messageKey, Object parameter1, Object parameter2) {
        return Translate.getString(messageKey, parameter1, parameter2, I2PAppContext.getGlobalContext(), BUNDLE_NAME);
    }
    
    /**
     * Removes all whitespace at the beginning and the end of a string,
     * and replaces multiple whitespace characters with a single space.
     * @param string
     */
    public static String removeExtraWhitespace(String string) {
        if (string == null)
            return null;
        return string.trim().replaceAll("\\s+", " ");
    }
    
    /**
     * Removes whitespace from the beginning and end of an address.
     * Also removes angle brackets if the address begins and ends
     * with an angle bracket.
     * @param address
     */
    public static String fixAddress(String address) {
        if (address == null)
            return null;
        
        address = address.trim();
        if (address.startsWith("<") && address.endsWith(">"))
            address = address.substring(1, address.length()-1);
        
        return address;
    }
    
    public static UniqueId zeroId() {
        return new UniqueId(new byte[UniqueId.LENGTH], 0);
    }
    
    public static String toBase32(Hash hash) {
        return Base32.encode(hash.toByteArray());
    }

    public static String toBase32(Destination destination) {
        return Base32.encode(destination.calculateHash().toByteArray());
    }

    public static String toShortenedBase32(Destination destination) {
        return toBase32(destination).substring(0, 8) + "...";
    }

    /**
     * Makes a file readable and writable only by the current OS user,
     * if the operating system supports it. Errors are ignored.
     * @param file
     */
    public static void makePrivate(File file) {
        file.setReadable(false, false);
        file.setReadable(true, true);
        file.setWritable(false, false);
        file.setWritable(true, true);
    }
    
    /** Encrypts data with an I2P public key */
    public static byte[] encrypt(byte data[], PublicKey key) {
        I2PAppContext appContext = I2PAppContext.getGlobalContext();
        SessionKey sessionKey = appContext.sessionKeyManager().createSession(key);
        return appContext.elGamalAESEngine().encrypt(data, key, sessionKey, null, null, null, 0);
    }
    
    /**
     * Decrypts data with an I2P private key 
     * @throws DataFormatException
     */
    public static byte[] decrypt(byte data[], PrivateKey key) throws DataFormatException {
        I2PAppContext appContext = I2PAppContext.getGlobalContext();
        return appContext.elGamalAESEngine().decrypt(data, key, appContext.sessionKeyManager());
    }
    
    /** Overwrites a <code>char</code> array with zeros */
    public static void zeroOut(char[] array) {
        for (int i=0; i<array.length; i++)
            array[i] = 0;
    }
}