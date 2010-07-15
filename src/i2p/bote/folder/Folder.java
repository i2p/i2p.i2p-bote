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

import i2p.bote.Util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import net.i2p.util.Log;

/**
 * 
 * @param <T> The type of objects the folder can store.
 */
public abstract class Folder<T> implements Iterable<T> {
    private Log log = new Log(Folder.class);
    protected File storageDir;
    protected String fileExtension;

    protected Folder(File storageDir, String fileExtension) {
        this.storageDir = storageDir;
        this.fileExtension = fileExtension;
        
        if (!storageDir.exists() && !storageDir.mkdirs())
            log.error("Can't create directory: '" + storageDir + "'");
        Util.makePrivate(storageDir);
    }

    public File getStorageDirectory() {
        return storageDir;
    }

    public int getNumElements() {
        return getFilenames().length;
    }
    
    /**
     * Returns the names of all files in the folder.
     * @return
     */
    public File[] getFilenames() {
        File[] files = storageDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toUpperCase().endsWith(fileExtension.toUpperCase());
            }
        });
        if (files == null)
            log.error("Cannot list files in directory <" + storageDir + ">");
        else
            // sort files by date, newest first
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    return (int)Math.signum(f2.lastModified() - f1.lastModified());
                }
            });
        
        return files;
    }
    
    public List<T> getElements() {
        List<T> elements = new ArrayList<T>();
        Iterator<T> iterator = iterator();
        while (iterator.hasNext())
            elements.add(iterator.next());
        return elements;
    }
    
    /** An {@link Iterator} implementation that loads one file into memory at a time. */
    @Override
    public final Iterator<T> iterator() {
        final File[] files = getFilenames();
        log.debug(files.length + " files with the extension '" + fileExtension + "' found in '" + storageDir + "'.");

        return new Iterator<T>() {
            int nextIndex = 0;
            File currentFile;
            
            @Override
            public boolean hasNext() {
                return nextIndex < files.length;
            }

            @Override
            public T next() {
                currentFile = files[nextIndex];
                nextIndex++;
                String filePath = currentFile.getAbsolutePath();
                log.debug("Reading file: '" + filePath + "'");
                try {
                    T nextElement = createFolderElement(currentFile);
                    return nextElement;
                }
                catch (Exception e) {
                    log.error("Can't create a FolderElement from file: " + filePath, e);
                    return null;
                }
            }
            
            @Override
            public void remove() {
                if (currentFile == null)
                    throw new IllegalStateException("remove() was called before next()");
                if (!currentFile.delete())
                    log.error("Can't delete file: <" + currentFile.getAbsolutePath() + ">");
            }
        };
    }
    
    protected abstract T createFolderElement(File file) throws Exception;
}