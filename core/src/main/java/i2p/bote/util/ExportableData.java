package i2p.bote.util;

import net.i2p.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.GeneralSecurityException;
import java.util.Properties;

import i2p.bote.I2PBote;
import i2p.bote.fileencryption.DerivedKey;
import i2p.bote.fileencryption.EncryptedInputStream;
import i2p.bote.fileencryption.EncryptedOutputStream;
import i2p.bote.fileencryption.PasswordCache;
import i2p.bote.fileencryption.PasswordException;

/**
 * Parent class for exportable data.
 */
public abstract class ExportableData {
    private Log log = new Log(ExportableData.class);

    protected abstract void initializeIfNeeded() throws PasswordException, IOException, GeneralSecurityException;

    public abstract void save() throws PasswordException, IOException, GeneralSecurityException;

    protected abstract boolean loadFromProperties(Properties properties, boolean append, boolean replace) throws GeneralSecurityException;

    protected abstract Properties saveToProperties() throws GeneralSecurityException;

    /**
     * Imports data from FileDescriptor.
     *
     * @param importFileDescriptor
     * @param password
     * @param append               Set to false if existing data should be dropped.
     * @param replace              Set to false to ignore duplicates, true to import and replace existing.
     * @return true if the import succeeded, false if no valid data was found.
     * @throws PasswordException        if the provided password is invalid.
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public boolean importFromFileDescriptor(FileDescriptor importFileDescriptor, String password,
                                            boolean append, boolean replace) throws PasswordException, IOException, GeneralSecurityException {
        initializeIfNeeded();

        InputStream importStream = new FileInputStream(importFileDescriptor);
        if (password != null)
            importStream = new EncryptedInputStream(importStream, password.getBytes());

        try {
            Properties properties = new Properties();
            properties.load(new InputStreamReader(importStream));

            if (loadFromProperties(properties, append, replace)) {
                // Save the new data
                save();
                return true;
            } else // No data found
                return false;
        } finally {
            importStream.close();
        }
    }

    public void export(File exportFile, String password) throws IOException, GeneralSecurityException, PasswordException {
        initializeIfNeeded();

        OutputStream exportStream = new FileOutputStream(exportFile);
        try {
            export(exportStream, password);
        } catch (IOException e) {
            log.error("Can't export data to file <" + exportFile.getAbsolutePath() + ">.", e);
            throw e;
        } finally {
            exportStream.close();
        }
    }

    public void export(OutputStream exportStream, String password) throws IOException, GeneralSecurityException, PasswordException {
        initializeIfNeeded();

        OutputStreamWriter writer;
        if (password != null) {
            // Use same salt and parameters as the on-disk files
            PasswordCache cache = new PasswordCache(I2PBote.getInstance().getConfiguration());
            cache.setPassword(password.getBytes());
            DerivedKey derivedKey = cache.getKey();
            writer = new OutputStreamWriter(new EncryptedOutputStream(exportStream, derivedKey), "UTF-8");
        } else
            writer = new OutputStreamWriter(exportStream, "UTF-8");

        Properties properties = saveToProperties();
        properties.store(writer, null);
        // If a password was provided, this call triggers the encryption
        writer.close();
    }
}
