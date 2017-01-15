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

import i2p.bote.fileencryption.PasswordException;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import net.i2p.util.Log;
import net.i2p.util.SecureFile;

/**
 * Reads files from a directory and returns them as objects of type <code>T</code>.<br/>
 * @param <T> The type of objects the folder can store.
 * @see PacketFolder
 * @see EmailFolder
 */
public abstract class Folder<T> {
    protected File storageDir;
    
    private Log log = new Log(Folder.class);
    private String fileExtension;

    /**
     * Creates a new <code>Folder</code> that handles files with a given extension.
     * @param storageDir
     * @param fileExtension
     */
    protected Folder(File storageDir, String fileExtension) {
        this.storageDir = storageDir;
        this.fileExtension = fileExtension;
        
        if (!storageDir.exists() && !new SecureFile(storageDir.getAbsolutePath()).mkdirs())
            log.error("Can't create directory: '" + storageDir + "'");
    }

    public File getStorageDirectory() {
        return storageDir;
    }

    /** Returns the unique name of this folder. */
    public String getName() {
        return storageDir.getName();
    }

    /**
     * Returns the number of elements in the folder, which is the size
     * of the array returned by {@link #getFilenames()}. This number may
     * differ from the total number of files in the folder.
     */
    public int getNumElements() {
        return getFilenames().length;
    }
    
    /**
     * Returns the names of all files in the folder that end in the <code>fileExtension</code>
     * value provided via the constructor.<br/>
     * If there are no such files, an empty array is returned.
     */
    protected File[] getFilenames() {
        File[] files = storageDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toUpperCase().endsWith(fileExtension.toUpperCase());
            }
        });
        if (files == null) {
            log.error("Cannot list files in directory <" + storageDir + ">");
            files = new File[0];
        } else {
            // sort files by date, newest first
            // This sort may be unstable, as the lastModified time of the files
            // may change during sorting (e.g. by a different thread), causing
            // Java's TimSort to throw an IAE.
            try {
                Arrays.sort(files, new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        return (int)Math.signum(f2.lastModified() - f1.lastModified());
                    }
                });
            } catch (IllegalArgumentException iae) {
                log.warn("Error while sorting files in " + storageDir + ", order will be unstable", iae);
            }
        }
        
        return files;
    }
    
    /**
     * Returns all folder elements as a {@link List}.
     * @throws PasswordException
     */
    public List<T> getElements() throws PasswordException {
        List<T> elements = new ArrayList<T>();
        FolderIterator<T> iterator = iterate();
        while (iterator.hasNext())
            elements.add(iterator.next());
        return elements;
    }
    
    /**
     * An {@link Iterator} implementation that loads one file into memory at a time.<br/>
     * Files that cannot be read or contain invalid data are skipped.
     */
    public final FolderIterator<T> iterate() {
        final File[] files = getFilenames();
        log.debug(files.length + " files with the extension '" + fileExtension + "' found in '" + storageDir + "'.");

        return new FolderIterator<T>() {
            Iterator<File> fileIterator = Arrays.asList(files).iterator();
            T nextElement;   // the next value to return
            File lastFile;   // the file that corresponds to the last element returned by next()
            File currentFile;   // the last file read in findNextElement()

            @Override
            public boolean hasNext() throws PasswordException {
                if (nextElement == null)
                    findNextElement();
                return nextElement != null;
            }

            @Override
            public T next() throws PasswordException {
                if (nextElement == null)
                    findNextElement();
                if (nextElement == null)
                    throw new NoSuchElementException("No more folder elements!");
                else {
                    lastFile = currentFile;
                    T retVal = nextElement;
                    findNextElement();
                    return retVal;
                }
            }
            
            /**
             * Reads the next valid file into <code>currentElement</code>.<br/>
             * If there are no more files, <code>currentElement</code> is set to <code>null</code>.
             * <p/>
             * <code>currentFile</code> is set to the last file read.
             * @param updateCurrentFile
             * @throws PasswordException
             */
            void findNextElement() throws PasswordException {
                while (fileIterator.hasNext()) {
                    currentFile = fileIterator.next();
                    String filePath = currentFile.getAbsolutePath();
                    log.debug("Reading file: '" + filePath + "'");
                    try {
                        nextElement = createFolderElement(currentFile);
                        if (nextElement != null)
                            return;
                    }
                    catch (PasswordException e) {
                        throw e;
                    }
                    catch (Exception e) {
                        log.error("Can't create a FolderElement from file: " + filePath + " (file size=" + currentFile.length() + ")", e);
                    }
                }
                nextElement = null;
            }
            
            @Override
            public void remove() {
                if (lastFile == null)
                    throw new IllegalStateException("remove() was called before next()");
                if (!lastFile.delete())
                    log.error("Can't delete file: <" + lastFile.getAbsolutePath() + ">");
            }
        };
    }
    
    /**
     * Reads a file from the filesystem and returns it as an object of type <code>T</code>.
     * @param file
     * @return the object, or null if it could not be created but an error shouldn't be logged.
     * @throws Exception to be logged if the object could not be created.
     */
    protected abstract T createFolderElement(File file) throws Exception;
    
    @Override
    public String toString() {
        return "Folder type=" + getClass().getSimpleName() + ", dir=" + storageDir.getAbsolutePath();
    }
}
