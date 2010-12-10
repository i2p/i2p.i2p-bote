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

package i2p.bote.io;

import static i2p.bote.io.FileEncryptionConstants.BLOCK_SIZE;
import static i2p.bote.io.FileEncryptionConstants.NUM_ITERATIONS;
import static i2p.bote.io.FileEncryptionConstants.SALT_LENGTH;
import static i2p.bote.io.FileEncryptionConstants.START_OF_FILE;
import static i2p.bote.io.FileEncryptionConstants.FORMAT_VERSION;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import net.i2p.I2PAppContext;
import net.i2p.data.SessionKey;

/**
 * Encrypts data with a password and writes it to an underlying {@link OutputStream}.<br/>
 * Nothing is actually written until {@link #close()} is called.<br/>
 * A header is written before the encrypted data. The header fields are:<br/>
 * <code>start of file, format version, #iterations, salt, iv, encrypted data</code>.<br/>
 */
public class EncryptedOutputStream extends FilterOutputStream {
    private OutputStream downstream;
    private char[] password;
    private ByteArrayOutputStream outputBuffer;
    
    /**
     * Creates an <code>EncryptedOutputStream</code> that encrypts data with a password obtained
     * from a <code>passwordHolder</code>.
     * @throws PasswordException 
     */
    public EncryptedOutputStream(OutputStream downstream, PasswordHolder passwordHolder) throws PasswordException {
        super(downstream);
        this.downstream = downstream;
        password = passwordHolder.getPassword();
        if (password == null)
            throw new PasswordException();
        password = passwordHolder.getPassword();
        outputBuffer = new ByteArrayOutputStream();
    }
    
    public EncryptedOutputStream(OutputStream downstream, char[] password) {
        super(downstream);
        this.downstream = downstream;
        this.password = password;
        outputBuffer = new ByteArrayOutputStream();
    }
    
    @Override
    public void write(int b) throws IOException {
        outputBuffer.write(b);
    }
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        outputBuffer.write(b, off, len);
    }
    
    public void close() throws IOException {
        try {
            downstream.write(START_OF_FILE);
            downstream.write(FORMAT_VERSION);
            byte[] numIterations = ByteBuffer.allocate(4).putInt(NUM_ITERATIONS).array();
            downstream.write(numIterations);
            
            I2PAppContext appContext = I2PAppContext.getGlobalContext();
            byte[] salt = new byte[SALT_LENGTH];
            appContext.random().nextBytes(salt);
            downstream.write(salt);
            
            byte iv[] = new byte[BLOCK_SIZE];
            appContext.random().nextBytes(iv);
            downstream.write(iv);
            
            byte[] data = outputBuffer.toByteArray();
            byte[] keyBytes = FileEncryptionUtil.getEncryptionKey(password, salt, NUM_ITERATIONS);
            SessionKey key = new SessionKey(keyBytes);
            byte[] encryptedData = appContext.aes().safeEncrypt(data, key, iv, 0);
            downstream.write(encryptedData);
        }
        catch(NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        catch(InvalidKeySpecException e) {
            throw new IOException(e);
        }
        finally {
            downstream.close();
        }
    }
}