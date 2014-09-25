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

package i2p.bote.folder;

import i2p.bote.UniqueId;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.i2p.util.Log;
import net.i2p.util.SecureFileOutputStream;

/**
 * Email packets are sometimes delivered again after the email has already
 * been received, because some storage nodes were offline the first time.
 * This class stores message IDs of received emails to avoid this problem.
 * 
 * File format: one message ID per line, sorted by the time the email was first
 * assembled, oldest to newest.
 * 
 * @see IncompleteEmailFolder
 */
public class MessageIdCache {
    private Log log = new Log(MessageIdCache.class);
    private File cacheFile;
    private int cacheSize;
    private List<UniqueId> idList;
    
    public MessageIdCache(File cacheFile, int sizecacheSize) {
        this.cacheFile = cacheFile;
        this.cacheSize = sizecacheSize;
        read(cacheFile);
    }
    
    private void read(File cacheFile) {
        idList = Collections.synchronizedList(new ArrayList<UniqueId>());
        if (!cacheFile.exists()) {
            log.debug("Message ID cache file doesn't exist: <" + cacheFile.getAbsolutePath() + ">");
            return;
        }
        
        log.debug("Reading message ID cache file: <" + cacheFile.getAbsolutePath() + ">");
        BufferedReader input = null;
        try {
            input = new BufferedReader(new FileReader(cacheFile));
            
            while (true) {
                String idString = input.readLine();
                if (idString == null)   // EOF
                    break;
                
                UniqueId id = new UniqueId(idString);
                idList.add(id);
            }
            
        }
        catch (IOException e) {
            log.error("Can't read message ID cache file.", e);
        }
        finally {
            if (input != null)
                try {
                    input.close();
                }
                catch (IOException e) {
                    log.error("Error closing BufferedReader.", e);
                }
        }
    }
    
    private void write(File cacheFile) {
        log.debug("Writing message ID cache file: <" + cacheFile.getAbsolutePath() + ">");
        String newLine = System.getProperty("line.separator");
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new SecureFileOutputStream(cacheFile.getAbsolutePath())));
            for (UniqueId id: idList)
                writer.write(id.toBase64() + newLine);
        }
        catch (IOException e) {
            log.error("Can't write message ID cache file.", e);
        }
        finally {
            if (writer != null)
                try {
                    writer.close();
                }
                catch (IOException e) {
                    log.error("Error closing Writer.", e);
                }
        }
    }
    
    void add(UniqueId messageId) {
        while (idList.size() > cacheSize)
            idList.remove(0);
        idList.add(messageId);
        // should not be too big a performance hit to write out the cache here because
        // this method only gets called when a genuinely new email is received
        write(cacheFile);
    }
    
    boolean contains(UniqueId messageId) {
        return idList.contains(messageId);
    }
}