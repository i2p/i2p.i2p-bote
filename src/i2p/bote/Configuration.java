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

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

import net.i2p.I2PAppContext;
import net.i2p.data.DataHelper;
import net.i2p.util.Log;
import net.i2p.util.Translate;

public class Configuration {
    private static final long serialVersionUID = -6318245413106186095L;
    private static final String I2P_BOTE_SUBDIR = "i2pbote";       // relative to the I2P app dir
    private static final String CONFIG_FILE_NAME = "i2pbote.config";
    private static final String DEST_KEY_FILE_NAME = "local_dest.key";
    private static final String PEER_FILE_NAME = "peers.txt";
    private static final String IDENTITIES_FILE_NAME = "identities.txt";
    private static final String ADDRESS_BOOK_FILE_NAME = "addressBook.txt";
    private static final String MESSAGE_ID_CACHE_FILE = "msgidcache.txt";
    private static final String OUTBOX_DIR = "outbox";              // relative to I2P_BOTE_SUBDIR
    private static final String OUTBOX_SUBDIR_LOCAL = "local";      // relative to OUTBOX_DIR
    private static final String OUTBOX_SUBDIR_RELAY = "relay";      // relative to OUTBOX_DIR
    private static final String INCOMPLETE_SUBDIR = "incomplete";   // relative to I2P_BOTE_SUBDIR
    private static final String EMAIL_DHT_SUBDIR = "dht_email_pkt";    // relative to I2P_BOTE_SUBDIR
    private static final String INDEX_PACKET_DHT_SUBDIR = "dht_index_pkt";    // relative to I2P_BOTE_SUBDIR
    private static final String INBOX_SUBDIR = "inbox";             // relative to I2P_BOTE_SUBDIR
    private static final String SENT_FOLDER_DIR = "sent";           // relative to I2P_BOTE_SUBDIR
    private static final String TRASH_FOLDER_DIR = "trash";         // relative to I2P_BOTE_SUBDIR

    // Parameter names in the config file
    private static final String PARAMETER_REDUNDANCY = "redundancy";
    private static final String PARAMETER_STORAGE_SPACE_INBOX = "storageSpaceInbox";
    private static final String PARAMETER_STORAGE_SPACE_RELAY = "storageSpaceRelay";
    private static final String PARAMETER_STORAGE_TIME = "storageTime";
    private static final String PARAMETER_MAX_FRAGMENT_SIZE = "maxFragmentSize";
    private static final String PARAMETER_HASHCASH_STRENGTH = "hashCashStrength";
    private static final String PARAMETER_SMTP_PORT = "smtpPort";
    private static final String PARAMETER_POP3_PORT = "pop3Port";
    private static final String PARAMETER_MAX_CONCURRENT_IDENTITIES_CHECK_MAIL = "maxConcurIdCheckMail";
    private static final String PARAMETER_AUTO_MAIL_CHECK = "autoMailCheckEnabled";
    private static final String PARAMETER_MAIL_CHECK_INTERVAL = "mailCheckInterval";
    private static final String PARAMETER_LANGUAGE = "locale";
    private static final String PARAMETER_HIDE_LOCALE = "hideLocale";
    private static final String PARAMETER_INCLUDE_SENT_TIME = "includeSentTime";
    private static final String PARAMETER_MESSAGE_ID_CACHE_SIZE = "messageIdCacheSize";
    
    // Defaults for each parameter
    private static final int DEFAULT_REDUNDANCY = 2;
    private static final int DEFAULT_STORAGE_SPACE_INBOX = 1024 * 1024 * 1024;
    private static final int DEFAULT_STORAGE_SPACE_RELAY = 100 * 1024 * 1024;
    private static final int DEFAULT_STORAGE_TIME = 31;   // in days
    private static final int DEFAULT_MAX_FRAGMENT_SIZE = 10 * 1024 * 1024;   // the maximum size one email fragment can be, in bytes
    private static final int DEFAULT_HASHCASH_STRENGTH = 10;
    private static final int DEFAULT_SMTP_PORT = 7661;
    private static final int DEFAULT_POP3_PORT = 7662;
    private static final int DEFAULT_MAX_CONCURRENT_IDENTITIES_CHECK_MAIL = 10;
    private static final boolean DEFAULT_AUTO_MAIL_CHECK = true;
    private static final int DEFAULT_MAIL_CHECK_INTERVAL = 30;   // in minutes
    private static final String DEFAULT_LANGUAGE = Locale.getDefault().getLanguage();
    private static final boolean DEFAULT_HIDE_LOCALE = true;
    private static final boolean DEFAULT_INCLUDE_SENT_TIME = true;
    private static final int DEFAULT_MESSAGE_ID_CACHE_SIZE = 1000;   // the maximum number of message IDs to cache
    
    private Log log = new Log(Configuration.class);
    private Properties properties;
    private File i2pBoteDir;
    private File configFile;
    
    /**
     * Reads configuration settings from the <code>I2P_BOTE_SUBDIR</code> subdirectory under
     * the I2P application directory. The I2P application directory can be changed via the
     * <code>i2p.dir.app</code> system property.
     * 
     * Logging is done through the I2P logger. I2P reads the log configuration from the
     * <code>logger.config</code> file whose location is determined by the
     * <code>i2p.dir.config</code> system property.
     * @return
     */
    public Configuration() {
        properties = new Properties();
        
        // get the I2PBote directory and make sure it exists
        i2pBoteDir = getI2PBoteDirectory();
        if (!i2pBoteDir.exists() && !i2pBoteDir.mkdirs())
        log.error("Cannot create directory: <" + i2pBoteDir.getAbsolutePath() + ">");
        
        // read the configuration file
        configFile = new File(i2pBoteDir, CONFIG_FILE_NAME);
        boolean configurationLoaded = false;
        if (configFile.exists()) {
            log.debug("Loading config file <" + configFile.getAbsolutePath() + ">");
            
            try {
                DataHelper.loadProps(properties, configFile);
                configurationLoaded = true;
            } catch (IOException e) {
                log.error("Error loading configuration file <" + configFile.getAbsolutePath() + ">", e);
            }
        }
        if (!configurationLoaded)
            log.info("Can't read configuration file <" + configFile.getAbsolutePath() + ">, using default settings.");
        
        // Apply the language setting if there is one in the config file
        setLanguage(getLanguage());
    }

    public File getDestinationKeyFile() {
        return new File(i2pBoteDir, DEST_KEY_FILE_NAME);
    }
    
    public File getPeerFile() {
        return new File(i2pBoteDir, PEER_FILE_NAME);
    }
    
    public File getIdentitiesFile() {
        return new File(i2pBoteDir, IDENTITIES_FILE_NAME);
    }
    
    public File getAddressBookFile() {
        return new File(i2pBoteDir, ADDRESS_BOOK_FILE_NAME);
    }
    
    public File getMessageIdCacheFile() {
        return new File(i2pBoteDir, MESSAGE_ID_CACHE_FILE);
    }
    
    public File getLocalOutboxDir() {
        return new File(getOutboxBaseDir(), OUTBOX_SUBDIR_LOCAL);	    
    }
    
    public File getRelayOutboxDir() {
        return new File(getOutboxBaseDir(), OUTBOX_SUBDIR_RELAY);       
    }
    
    private File getOutboxBaseDir() {
        return new File(i2pBoteDir, OUTBOX_DIR);
    }
    
    public File getSentFolderDir() {
        return new File(i2pBoteDir, SENT_FOLDER_DIR);
    }
    
    public File getTrashFolderDir() {
        return new File(i2pBoteDir, TRASH_FOLDER_DIR);
    }
    
    public File getInboxDir() {
        return new File(i2pBoteDir, INBOX_SUBDIR);       
    }
    
    public File getIncompleteDir() {
        return new File(i2pBoteDir, INCOMPLETE_SUBDIR);       
    }
    
    public File getEmailDhtStorageDir() {
        return new File(i2pBoteDir, EMAIL_DHT_SUBDIR);       
    }
    
    public File getIndexPacketDhtStorageDir() {
        return new File(i2pBoteDir, INDEX_PACKET_DHT_SUBDIR);
    }
    
    private static File getI2PBoteDirectory() {
        // the parent directory of the I2PBote directory ($HOME or the value of the i2p.dir.app property)
        File i2pAppDir = I2PAppContext.getGlobalContext().getAppDir();
        
        return new File(i2pAppDir, I2P_BOTE_SUBDIR);
    }
    
    /**
     * Save the configuration
     * @param configFile
     */
    public void save() {
    	log.debug("Saving config file <" + configFile.getAbsolutePath() + ">");
    	try {
            DataHelper.storeProps(properties, configFile);
        } catch (IOException e) {
            log.error("Cannot save configuration to file <" + configFile.getAbsolutePath() + ">", e);
        }
    }

    /**
     * Returns the number of relays to use for sending and receiving email. Zero is a legal value.
     * @return
     */
    public int getRedundancy() {
    	return getIntParameter(PARAMETER_REDUNDANCY, DEFAULT_REDUNDANCY);
    }

    /**
     * Returns the maximum size (in bytes) the inbox can take up.
     * @return
     */
    public int getStorageSpaceInbox() {
    	return getIntParameter(PARAMETER_STORAGE_SPACE_INBOX, DEFAULT_STORAGE_SPACE_INBOX);
    }
    
    /**
     * Returns the maximum size (in bytes) all messages stored for relaying can take up.
     * @return
     */
    public int getStorageSpaceRelay() {
    	return getIntParameter(PARAMETER_STORAGE_SPACE_RELAY, DEFAULT_STORAGE_SPACE_RELAY);
    }
    
    /**
     * Returns the time (in milliseconds) after which an email is deleted from the outbox if it cannot be sent / relayed.
     * @return
     */
    public long getStorageTime() {
    	return 24L * 3600 * 1000 * getIntParameter(PARAMETER_STORAGE_TIME, DEFAULT_STORAGE_TIME);
    }

    public int getMaxFragmentSize() {
    	return getIntParameter(PARAMETER_MAX_FRAGMENT_SIZE, DEFAULT_MAX_FRAGMENT_SIZE);
    }
    
    public int getHashCashStrength() {
    	return getIntParameter(PARAMETER_HASHCASH_STRENGTH, DEFAULT_HASHCASH_STRENGTH);
    }
    
    /**
     * Returns the maximum number of email identities to retrieve new emails for at a time.
     * @return
     */
    public int getMaxConcurIdCheckMail() {
        return getIntParameter(PARAMETER_MAX_CONCURRENT_IDENTITIES_CHECK_MAIL, DEFAULT_MAX_CONCURRENT_IDENTITIES_CHECK_MAIL);
    }
    
    public void setAutoMailCheckEnabled(boolean enabled) {
        properties.setProperty(PARAMETER_AUTO_MAIL_CHECK, new Boolean(enabled).toString());
    }
    
    public boolean isAutoMailCheckEnabled() {
        return getBooleanParameter(PARAMETER_AUTO_MAIL_CHECK, DEFAULT_AUTO_MAIL_CHECK);
    }
    
    public void setMailCheckInterval(int minutes) {
        properties.setProperty(PARAMETER_MAIL_CHECK_INTERVAL, new Integer(minutes).toString());
    }
    
    /**
     * Returns the number of minutes the application should wait before automatically
     * checking for mail.
     * @return
     */
    public int getMailCheckInterval() {
        return getIntParameter(PARAMETER_MAIL_CHECK_INTERVAL, DEFAULT_MAIL_CHECK_INTERVAL);
    }

    /**
     * Sets the UI language.
     * @param languageCode A two-letter language code such as "en" or "de", or null for the system default.
     */
    public void setLanguage(String languageCode) {
        if (languageCode == null) {
            properties.remove(PARAMETER_LANGUAGE);
            System.clearProperty(Translate.PROP_LANG);
        }
        else {
            properties.setProperty(PARAMETER_LANGUAGE, languageCode);

            // Set the language used by i2p.bote.Util._().
            // This may interfere with other I2P apps.
            // Unfortunately, there is no setProperty() in I2PAppContext.
            System.setProperty(Translate.PROP_LANG, languageCode);
        }
    }

    /**
     * Returns the two-letter language code for the current locale.
     * @return
     */
    public String getLanguage() {
        return properties.getProperty(PARAMETER_LANGUAGE, DEFAULT_LANGUAGE);
    }

    /**
     * Returns the current locale.
     * @return
     */
    public Locale getLocale() {
        return new Locale(getLanguage());
    }

    /**
     * Returns all locales for which a translation exists.
     * @return
     */
    public Locale[] getAllLocales() {
        return I2PBote.getInstance().getAllLocales();
    }

    /**
     * Controls whether strings that are added to outgoing email, like "Re:" or "Fwd:",
     * are translated or not.
     * If <code>hideLocale</code> is <code>false</code>, the UI language is used.
     * If <code>hideLocale</code> is <code>true</code>, the strings are left untranslated
     * (which means they are in English).
     * @param hideLocale
     */
    public void setHideLocale(boolean hideLocale) {
        properties.setProperty(PARAMETER_HIDE_LOCALE, new Boolean(hideLocale).toString());
    }
    
    public boolean getHideLocale() {
        return getBooleanParameter(PARAMETER_HIDE_LOCALE, DEFAULT_HIDE_LOCALE);
    }

    /**
     * Controls whether the send time is included in outgoing emails.
     * @param includeSentTime
     */
    public void setIncludeSentTime(boolean includeSentTime) {
        properties.setProperty(PARAMETER_INCLUDE_SENT_TIME, new Boolean(includeSentTime).toString());
    }
    
    public boolean getIncludeSentTime() {
        return getBooleanParameter(PARAMETER_INCLUDE_SENT_TIME, DEFAULT_INCLUDE_SENT_TIME);
    }
    
    public int getMessageIdCacheSize() {
        return getIntParameter(PARAMETER_MESSAGE_ID_CACHE_SIZE, DEFAULT_MESSAGE_ID_CACHE_SIZE);
    }
    
    private boolean getBooleanParameter(String parameterName, boolean defaultValue) {
        String stringValue = properties.getProperty(parameterName);
        if ("true".equalsIgnoreCase(stringValue) || "yes".equalsIgnoreCase(stringValue) || "on".equalsIgnoreCase(stringValue) || "1".equals(stringValue))
            return true;
        else if ("false".equalsIgnoreCase(stringValue) || "no".equalsIgnoreCase(stringValue) || "off".equalsIgnoreCase(stringValue) || "0".equals(stringValue))
            return false;
        else if (stringValue == null)
            return defaultValue;
        else {
            log.warn("<" + stringValue + "> is not a legal value for the boolean parameter <" + parameterName + ">");
            return defaultValue;
        }
    }
    
    private int getIntParameter(String parameterName, int defaultValue) {
    	String stringValue = properties.getProperty(parameterName);
    	if (stringValue == null)
    		return defaultValue;
    	else
    		try {
    			return new Integer(stringValue);
    		}
    		catch (NumberFormatException e) {
    			log.warn("Can't convert value <" + stringValue + "> for parameter <" + parameterName + "> to int, using default.");
    			return defaultValue;
    		}
    }
}
