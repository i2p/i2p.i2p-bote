/**
 * Copyright (C) 2014  str4d@mail.i2p
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

package i2p.bote.email;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.activation.MimetypesFileTypeMap;

import net.i2p.util.Log;

public class FileAttachment implements Attachment {
    private Log log = new Log(FileAttachment.class);
    private String origFilename;
    private String tempFilename;
    private String mimeType;
    private DataHandler dataHandler;

    public FileAttachment(String origFilename, String tempFilename) {
        this.origFilename = origFilename;
        this.tempFilename = tempFilename;
        loadMimeType();
        dataHandler = new DataHandler(new FileDataSource(tempFilename) {
            @Override
            public String getContentType() {
                return mimeType;
            }
        });
    }

    /**
     * Returns the MIME type for an <code>Attachment</code>. MIME detection is done with
     * JRE classes, so only a small number of MIME types are supported.<p/>
     * It might be worthwhile to use the mime-util library which does a much better job:
     * {@link http://sourceforge.net/projects/mime-util/files/}.
     */
    private void loadMimeType() {
        MimetypesFileTypeMap mimeTypeMap = new MimetypesFileTypeMap();
        mimeType = mimeTypeMap.getContentType(origFilename);
        if (!"application/octet-stream".equals(mimeType))
            return;

        InputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(tempFilename));
            mimeType = URLConnection.guessContentTypeFromStream(inputStream);
            if (mimeType != null)
                return;
        } catch (IOException e) {
            log.error("Can't read file: <" + tempFilename + ">", e);
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                log.error("Can't close file: <" + tempFilename + ">", e);
            }
        }

        mimeType = "application/octet-stream";
    }

    @Override
    public String getFileName() {
        return origFilename;
    }

    @Override
    public DataHandler getDataHandler() {
        return dataHandler;
    }

    @Override
    public boolean clean() {
        File tempFile = new File(tempFilename);
        boolean success = tempFile.delete();
        if (!success)
            log.error("Can't delete file: <" + tempFile.getAbsolutePath() + ">");
        return success;
    }
}