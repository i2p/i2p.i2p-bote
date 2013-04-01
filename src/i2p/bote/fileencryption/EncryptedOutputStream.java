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

package i2p.bote.fileencryption;

import static i2p.bote.fileencryption.FileEncryptionConstants.BLOCK_SIZE;
import static i2p.bote.fileencryption.FileEncryptionConstants.FORMAT_VERSION;
import static i2p.bote.fileencryption.FileEncryptionConstants.START_OF_FILE;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import net.i2p.I2PAppContext;
import net.i2p.data.SessionKey;

/**
 * Encrypts data with a password and writes it to an underlying {@link OutputStream}.<br/>
 * Nothing is actually written until {@link #close()} is called.<br/>
 * Nothing is written when <code>flush</code> is called, either, because if more data were
 * written after a <code>flush</code>, it would cause data corruption because the stream
 * would be encrypted two separate pieces.
 * <p/>
 * A header is written before the encrypted data. The header fields are:<br/>
 * <code>start of file, format version, scrypt parameters (N, r, p), salt, iv, encrypted data</code>.
 */
public class EncryptedOutputStream extends FilterOutputStream {
    private OutputStream downstream;
    private DerivedKey derivedKey;
    private ByteArrayOutputStream outputBuffer;
    
    /**
     * Creates an <code>EncryptedOutputStream</code> that encrypts data with a password obtained
     * from a <code>PasswordHolder</code>.
     * @throws PasswordException 
     * @throws IOException 
     * @throws GeneralSecurityException 
     */
    public EncryptedOutputStream(OutputStream downstream, PasswordHolder passwordHolder) throws PasswordException, IOException, GeneralSecurityException {
        super(downstream);
        this.downstream = downstream;
        byte[] password = passwordHolder.getPassword();
        if (password == null)
            throw new PasswordException();
        try {
            // make a copy in case PasswordCache zeros out the password before close() is called
            derivedKey = passwordHolder.getKey().clone();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        } catch (InvalidKeySpecException e) {
            throw new IOException(e);
        }
        outputBuffer = new ByteArrayOutputStream();
    }
    
    public EncryptedOutputStream(OutputStream downstream, DerivedKey derivedKey) {
        super(downstream);
        this.downstream = downstream;
        this.derivedKey = derivedKey.clone();
        outputBuffer = new ByteArrayOutputStream();
    }
    
    @Override
    public void write(int b) throws IOException {
        outputBuffer.write(b);
    }
    
    @Override
    public void write(byte[] b, int off, int len) {
        outputBuffer.write(b, off, len);
    }
    
    @Override
    public void close() throws IOException {
        try {
            encryptAndWrite();
        }
        finally {
            // erase the copy of the key
            derivedKey.clear();
            derivedKey = null;
            
            downstream.close();
        }
    }
    
    /**
     * Writes the header, then encrypts the internal buffer and writes the encrypted
     * data to the underlying <code>OutputStream</code>.
     * @throws IOException
     */
    @SuppressWarnings("deprecation") // for net.i2p.crypto.AESEngine
    private void encryptAndWrite() throws IOException {
        downstream.write(START_OF_FILE);
        downstream.write(FORMAT_VERSION);
        FileEncryptionConstants.KDF_PARAMETERS.writeTo(downstream);
        
        downstream.write(derivedKey.salt);
        
        byte iv[] = new byte[BLOCK_SIZE];
        I2PAppContext appContext = I2PAppContext.getGlobalContext();
        appContext.random().nextBytes(iv);
        downstream.write(iv);
        
        byte[] data = outputBuffer.toByteArray();
        SessionKey key = new SessionKey(derivedKey.key);
        byte[] encryptedData = appContext.aes().safeEncrypt(data, key, iv, 0);
        downstream.write(encryptedData);
    }
}
