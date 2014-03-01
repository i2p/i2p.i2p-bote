package i2p.bote.imap;

import org.apache.commons.configuration.ConfigurationException;

import i2p.bote.Configuration;
import i2p.bote.fileencryption.PasswordVerifier;
import i2p.bote.folder.EmailFolderManager;

/**
 * Stubbed-out ImapService
 */
public class ImapService {
    public ImapService(Configuration configuration, final PasswordVerifier passwordVerifier, EmailFolderManager folderManager) throws ConfigurationException {
    }

    public boolean isStarted() {
        return false;
    }

    public boolean start() {
        return false;
    }

    public boolean stop() {
        return true;
    }
}
