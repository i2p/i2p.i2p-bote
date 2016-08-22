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

import i2p.bote.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

/**
 * A command line program for decrypting I2P-Bote files.
 */
public class Decrypt {
    
    private Decrypt(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }
        
        File inputFile = new File(args[0]);
        if (!inputFile.exists()) {
            System.err.println("File not found: " + inputFile.getAbsolutePath());
            System.exit(1);
        }
        
        byte[] password = promptForPassword();
        if (password == null)
            System.exit(0);
        
        InputStream fileInputStream = null;
        OutputStream output = null;
        try {
            fileInputStream = new FileInputStream(inputFile);
            InputStream encryptedInputStream = new EncryptedInputStream(fileInputStream, password);
            
            if (args.length < 2)
                Util.copy(encryptedInputStream, System.out);
            else {
                File outputFile = new File(args[1]);
                output = new FileOutputStream(outputFile);
                Util.copy(encryptedInputStream, output);
            }
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getLocalizedMessage());
            System.exit(1);
        } catch (GeneralSecurityException e) {
            System.err.println("Error: " + e.getLocalizedMessage());
            System.exit(1);
        } catch (PasswordException e) {
            System.err.println("Wrong password.");
            System.exit(1);
        } finally {
            if (fileInputStream != null)
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    System.err.println("Error closing input file.");
                }
            if (output != null)
                try {
                    output.close();
                } catch (IOException e) {
                    System.err.println("Error closing output file.");
                }
        }
    }
    
    private void printUsage() {
        System.out.println("Syntax: Decrypt <input file> [output file]");
        System.out.println();
        System.out.println("Decrypts an input file and writes it to an output file.");
        System.out.println("Existing files are overwritten without warning.");
        System.out.println("If no output file is given, stdout is used instead.");
    }
    
    private byte[] promptForPassword() {
        System.out.print("Enter I2P-Bote password: ");
        char[] passwordChars = System.console().readPassword();
        if (passwordChars != null)
            return new String(passwordChars).getBytes();
        else
            return null;
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        new Decrypt(args);
    }
}