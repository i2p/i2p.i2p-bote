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

import net.i2p.I2PAppContext;
import net.i2p.crypto.KeyStoreUtil;
import net.i2p.data.DataHelper;
import net.i2p.util.Log;
import net.i2p.util.SecureFile;
import net.i2p.util.SystemVersion;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import i2p.bote.email.EmailIdentity.IdentityConfig;
import i2p.bote.packet.dht.Contact;

public class Configuration implements IdentityConfig {
    public static final String KEY_DERIVATION_PARAMETERS_FILE = "derivparams";   // name of the KDF parameter cache file, relative to I2P_BOTE_SUBDIR

    private static final String I2P_BOTE_SUBDIR = "i2pbote";       // relative to the I2P app dir
    private static final String CONFIG_FILE_NAME = "i2pbote.config";
    private static final String DEST_KEY_FILE_NAME = "local_dest.key";
    private static final String DHT_PEER_FILE_NAME = "dht_peers.txt";
    private static final String RELAY_PEER_FILE_NAME = "relay_peers.txt";
    private static final String IDENTITIES_FILE_NAME = "identities";
    private static final String ADDRESS_BOOK_FILE_NAME = "addressBook";
    private static final String MESSAGE_ID_CACHE_FILE = "msgidcache.txt";
    private static final String PASSWORD_FILE = "password";
    private static final String SSL_KEYSTORE_FILE = "i2p.bote.ssl.keystore.jks";      // relative to I2P_BOTE_SUBDIR
    private static final String SSL_KEY_ALIAS = "botessl";
    private static final String OUTBOX_DIR = "outbox";              // relative to I2P_BOTE_SUBDIR
    private static final String RELAY_PKT_SUBDIR = "relay_pkt";     // relative to I2P_BOTE_SUBDIR
    private static final String INCOMPLETE_SUBDIR = "incomplete";   // relative to I2P_BOTE_SUBDIR
    private static final String EMAIL_DHT_SUBDIR = "dht_email_pkt";    // relative to I2P_BOTE_SUBDIR
    private static final String INDEX_PACKET_DHT_SUBDIR = "dht_index_pkt";    // relative to I2P_BOTE_SUBDIR
    private static final String DIRECTORY_ENTRY_DHT_SUBDIR = "dht_directory_pkt";    // relative to I2P_BOTE_SUBDIR
    private static final String INBOX_SUBDIR = "inbox";             // relative to I2P_BOTE_SUBDIR
    private static final String SENT_FOLDER_DIR = "sent";           // relative to I2P_BOTE_SUBDIR
    private static final String TRASH_FOLDER_DIR = "trash";         // relative to I2P_BOTE_SUBDIR
    private static final String MIGRATION_VERSION_FILE = "migratedVersion";   // relative to I2P_BOTE_SUBDIR
    private static final List<Theme> BUILT_IN_THEMES = Arrays.asList(new Theme[] {   // theme IDs correspond to a theme directory in the .war
            new Theme("material", "Material"),
            new Theme("lblue", "Light Blue"),
            new Theme("vanilla", "Vanilla")
    });
    private static final String THEME_SUBDIR = "themes";   // relative to I2P_BOTE_SUBDIR

    // Parameter names in the config file
    private static final String PARAMETER_STORAGE_SPACE_INBOX = "storageSpaceInbox";
    private static final String PARAMETER_STORAGE_SPACE_RELAY = "storageSpaceRelay";
    private static final String PARAMETER_STORAGE_TIME = "storageTime";
    private static final String PARAMETER_HASHCASH_STRENGTH = "hashCashStrength";
    private static final String PARAMETER_SMTP_PORT = "smtpPort";
    private static final String PARAMETER_SMTP_ADDRESS = "smtpAddress";
    private static final String PARAMETER_SMTP_ENABLED = "smtpEnabled";
    private static final String PARAMETER_IMAP_PORT = "imapPort";
    private static final String PARAMETER_IMAP_ADDRESS = "imapAddress";
    private static final String PARAMETER_IMAP_ENABLED = "imapEnabled";
    private static final String PARAMETER_SSL_KEYSTORE_PASSWORD = "sslKeystorePassword";
    private static final String PARAMETER_MAX_CONCURRENT_IDENTITIES_CHECK_MAIL = "maxConcurIdCheckMail";
    private static final String PARAMETER_AUTO_MAIL_CHECK = "autoMailCheckEnabled";
    private static final String PARAMETER_DELIVERY_CHECK = "deliveryCheckEnabled";
    private static final String PARAMETER_MAIL_CHECK_INTERVAL = "mailCheckInterval";
    private static final String PARAMETER_OUTBOX_CHECK_INTERVAL = "outboxCheckInterval";
    private static final String PARAMETER_DELIVERY_CHECK_INTERVAL = "deliveryCheckInterval";
    private static final String PARAMETER_RELAY_SEND_PAUSE = "RelaySendPause";
    private static final String PARAMETER_HIDE_LOCALE = "hideLocale";
    private static final String PARAMETER_INCLUDE_SENT_TIME = "includeSentTime";
    private static final String PARAMETER_MESSAGE_ID_CACHE_SIZE = "messageIdCacheSize";
    private static final String PARAMETER_RELAY_REDUNDANCY = "relayRedundancy";
    private static final String PARAMETER_RELAY_MIN_DELAY = "relayMinDelay";
    private static final String PARAMETER_RELAY_MAX_DELAY = "relayMaxDelay";
    private static final String PARAMETER_NUM_STORE_HOPS = "numSendHops";
    private static final String PARAMETER_GATEWAY_DESTINATION = "gatewayDestination";
    private static final String PARAMETER_GATEWAY_ENABLED = "gatewayEnabled";
    private static final String PARAMETER_PASSWORD_CACHE_DURATION = "passwordCacheDuration";
    private static final String PARAMETER_EEPROXY_HOST = "eeproxyHost";
    private static final String PARAMETER_EEPROXY_PORT = "eeproxyPort";
    private static final String PARAMETER_UPDATE_URL = "updateUrl";
    private static final String PARAMETER_UPDATE_CHECK_INTERVAL = "updateCheckInterval";
    private static final String PARAMETER_THEME = "theme";

    // Defaults for each parameter
    private static final int DEFAULT_STORAGE_SPACE_INBOX = 1024 * 1024 * 1024;
    private static final int DEFAULT_STORAGE_SPACE_RELAY = 100 * 1024 * 1024;
    private static final int DEFAULT_STORAGE_TIME = 31;   // in days
    private static final int DEFAULT_HASHCASH_STRENGTH = 10;
    private static final int DEFAULT_SMTP_PORT = 7661;
    private static final String DEFAULT_SMTP_ADDRESS = "localhost";
    private static final boolean DEFAULT_SMTP_ENABLED = false;
    private static final int DEFAULT_IMAP_PORT = 7662;
    private static final String DEFAULT_IMAP_ADDRESS = "localhost";
    private static final boolean DEFAULT_IMAP_ENABLED = false;
    private static final int DEFAULT_MAX_CONCURRENT_IDENTITIES_CHECK_MAIL = 10;
    private static final boolean DEFAULT_AUTO_MAIL_CHECK = true;
    private static final int DEFAULT_MAIL_CHECK_INTERVAL = 30;   // in minutes
    private static final int DEFAULT_OUTBOX_CHECK_INTERVAL = 10;   // in minutes
    private static final boolean DEFAULT_DELIVERY_CHECK = true;
    private static final int DEFAULT_DELIVERY_CHECK_INTERVAL = 60;   // in minutes
    private static final int DEFAULT_RELAY_SEND_PAUSE = 10;   // in minutes, see RelayPacketSender.java
    private static final boolean DEFAULT_HIDE_LOCALE = true;
    private static final boolean DEFAULT_INCLUDE_SENT_TIME = true;
    private static final int DEFAULT_MESSAGE_ID_CACHE_SIZE = 1000;   // the maximum number of message IDs to cache
    private static final int DEFAULT_RELAY_REDUNDANCY = 5;   // lower than the DHT redundancy because only the highest-uptime peers are used for relaying
    private static final int DEFAULT_RELAY_MIN_DELAY = 5;   // in minutes
    private static final int DEFAULT_RELAY_MAX_DELAY = 40;   // in minutes
    private static final int DEFAULT_NUM_STORE_HOPS = 2;
    private static final String DEFAULT_GATEWAY_DESTINATION = "";
    private static final boolean DEFAULT_GATEWAY_ENABLED = true;
    private static final int DEFAULT_PASSWORD_CACHE_DURATION = 10;   // in minutes
    private static final String DEFAULT_EEPROXY_HOST = "localhost";
    private static final int DEFAULT_EEPROXY_PORT = 4444;
    private static final String DEFAULT_UPDATE_URL = "http://tjgidoycrw6s3guetge3kvrvynppqjmvqsosmtbmgqasa6vmsf6a.b32.i2p/i2pbote-update.xpi2p";
    private static final int DEFAULT_UPDATE_CHECK_INTERVAL = 60;   // in minutes
    private static final String DEFAULT_THEME = "material";
    
    // I2CP parameters allowed in the config file
    // Undefined parameters use the I2CP defaults
    private static final String PARAMETER_I2CP_DOMAIN_SOCKET = "i2cp.domainSocket";
    private static final List<String> I2CP_PARAMETERS = Arrays.asList(new String[] {
            PARAMETER_I2CP_DOMAIN_SOCKET,
            "inbound.length",
            "inbound.lengthVariance",
            "inbound.quantity",
            "inbound.backupQuantity",
            "outbound.length",
            "outbound.lengthVariance",
            "outbound.quantity",
            "outbound.backupQuantity",
    });

    private Log log = new Log(Configuration.class);
    private Properties properties;
    private File i2pBoteDir;
    private File configFile;

    /**
     * Reads configuration settings from the <code>I2P_BOTE_SUBDIR</code> subdirectory under
     * the I2P application directory. The I2P application directory can be changed via the
     * <code>i2p.dir.app</code> system property.
     * <p/>
     * Logging is done through the I2P logger. I2P reads the log configuration from the
     * <code>logger.config</code> file whose location is determined by the
     * <code>i2p.dir.config</code> system property.
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
            log.info("Loading config file <" + configFile.getAbsolutePath() + ">");

            try {
                DataHelper.loadProps(properties, configFile);
                configurationLoaded = true;
            } catch (IOException e) {
                log.error("Error loading configuration file <" + configFile.getAbsolutePath() + ">", e);
            }
        }
        if (!configurationLoaded)
            log.info("Can't read configuration file <" + configFile.getAbsolutePath() + ">, using default settings.");

        // Create SSL key if necessary
        if (!SystemVersion.isAndroid()) {
            File ks = getSSLKeyStoreFile();
            if (!ks.exists())
                createKeyStore(ks);
        }
    }

    private boolean createKeyStore(File ks) {
        // make a random 48 character password (30 * 8 / 5)
        String keyStorePassword = KeyStoreUtil.randomString();
        // and one for the cname
        String cname = KeyStoreUtil.randomString() + ".ssl.bote.i2p";

        boolean success = KeyStoreUtil.createKeys(
                ks, keyStorePassword, SSL_KEY_ALIAS, cname, "I2P-Bote",
                3652, "RSA", 2048, keyStorePassword);
        if (success) {
            success = ks.exists();
            if (success) {
                properties.setProperty(PARAMETER_SSL_KEYSTORE_PASSWORD, keyStorePassword);
                save();
            }
        }
        if (success) {
            log.logAlways(Log.INFO, "Created self-signed certificate for " + cname + " in keystore: " + ks.getAbsolutePath() + "\n" +
                    "The certificate name was generated randomly, and is not associated with your " +
                    "IP address, host name, router identity, or destination keys.");
        } else {
            log.error("Failed to create I2P-Bote SSL keystore.\n" +
                    "This is for the Sun/Oracle keytool, others may be incompatible.\n" +
                    "If you create the keystore manually, you must add " + PARAMETER_SSL_KEYSTORE_PASSWORD +
                    " to " + (new File(i2pBoteDir, CONFIG_FILE_NAME)).getAbsolutePath() + "\n" +
                    "You must create the keystore using the same password for the keystore and the key.");
        }
        return success;
    }

    /**
     * @param enabled ignored if not on Android.
     * @since 0.2.10
     */
    public void setI2CPDomainSocket(String name) {
        if (SystemVersion.isAndroid())
            properties.setProperty(
                    PARAMETER_I2CP_DOMAIN_SOCKET, name);
    }

    /**
     * @return a Properties containing the current I2CP options.
     * @since 0.4.3
     */
    public Properties getI2CPOptions() {
        Properties opts = new Properties();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (I2CP_PARAMETERS.contains(entry.getKey()))
                opts.put(entry.getKey(), entry.getValue());
        }
        return opts;
    }

    public File getDestinationKeyFile() {
        return new File(i2pBoteDir, DEST_KEY_FILE_NAME);
    }

    public File getDhtPeerFile() {
        return new File(i2pBoteDir, DHT_PEER_FILE_NAME);
    }

    public File getRelayPeerFile() {
        return new File(i2pBoteDir, RELAY_PEER_FILE_NAME);
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

    /**
     * The file returned by this method does not contain the user's password,
     * but a known string that is encrypted with the password. The purpose
     * of this file is for checking if a password entered by the user is
     * correct.
     */
    public File getPasswordFile() {
        return new File(i2pBoteDir, PASSWORD_FILE);
    }

    /**
     * Returns the file that caches the parameters needed for generating a
     * file encryption key from a password.
     */
    public File getKeyDerivationParametersFile() {
        return new File(i2pBoteDir, KEY_DERIVATION_PARAMETERS_FILE);
    }

    /**
     * @return the keystore file containing the SSL server key.
     * @since 0.2.10
     */
    public File getSSLKeyStoreFile() {
        return new File(i2pBoteDir, SSL_KEYSTORE_FILE);
    }

    public File getOutboxDir() {
        return new File(i2pBoteDir, OUTBOX_DIR);        
    }

    public File getRelayPacketDir() {
        return new File(i2pBoteDir, RELAY_PKT_SUBDIR);       
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

    /** Returns the directory where DHT packets of type {@link Contact} are stored. */
    public File getDirectoryEntryDhtStorageDir() {
        return new File(i2pBoteDir, DIRECTORY_ENTRY_DHT_SUBDIR);
    }

    private static File getI2PBoteDirectory() {
        // the parent directory of the I2PBote directory ($HOME or the value of the i2p.dir.app property)
        File i2pAppDir = I2PAppContext.getGlobalContext().getAppDir();

        return new File(i2pAppDir, I2P_BOTE_SUBDIR);
    }

    /**
     * Saves the configuration to a file.
     */
    public void save() {
        log.debug("Saving config file <" + configFile.getAbsolutePath() + ">");
        try {
            DataHelper.storeProps(properties, new SecureFile(configFile.getAbsolutePath()));
        } catch (IOException e) {
            log.error("Cannot save configuration to file <" + configFile.getAbsolutePath() + ">", e);
        }
    }

    /**
     * Returns the maximum size (in bytes) the inbox can take up.
     */
    public int getStorageSpaceInbox() {
        return getIntParameter(PARAMETER_STORAGE_SPACE_INBOX, DEFAULT_STORAGE_SPACE_INBOX);
    }

    /**
     * Returns the maximum size (in bytes) all messages stored for relaying can take up.
     */
    public int getStorageSpaceRelay() {
        return getIntParameter(PARAMETER_STORAGE_SPACE_RELAY, DEFAULT_STORAGE_SPACE_RELAY);
    }

    /**
     * Returns the time (in milliseconds) after which an email is deleted from the outbox if it cannot be sent or relayed.
     */
    public long getStorageTime() {
        return 24L * 3600 * 1000 * getIntParameter(PARAMETER_STORAGE_TIME, DEFAULT_STORAGE_TIME);
    }

    public int getHashCashStrength() {
        return getIntParameter(PARAMETER_HASHCASH_STRENGTH, DEFAULT_HASHCASH_STRENGTH);
    }

    public void setSmtpPort(int port) {
        properties.setProperty(PARAMETER_SMTP_PORT, String.valueOf(port));
    }

    public int getSmtpPort() {
        return getIntParameter(PARAMETER_SMTP_PORT, DEFAULT_SMTP_PORT);
    }

    /** Returns the host name the SMTP server listens on. */
    public String getSmtpAddress() {
        return properties.getProperty(PARAMETER_SMTP_ADDRESS, DEFAULT_SMTP_ADDRESS);
    }

    public void setSmtpEnabled(boolean enabled) {
        properties.setProperty(PARAMETER_SMTP_ENABLED, String.valueOf(enabled));
    }

    public boolean isSmtpEnabled() {
        return getBooleanParameter(PARAMETER_SMTP_ENABLED, DEFAULT_SMTP_ENABLED);
    }

    public void setImapPort(int port) {
        properties.setProperty(PARAMETER_IMAP_PORT, String.valueOf(port));
    }

    public int getImapPort() {
        return getIntParameter(PARAMETER_IMAP_PORT, DEFAULT_IMAP_PORT);
    }

    /** Returns the host name the IMAP server listens on. */
    public String getImapAddress() {
        return properties.getProperty(PARAMETER_IMAP_ADDRESS, DEFAULT_IMAP_ADDRESS);
    }

    public void setImapEnabled(boolean enabled) {
        properties.setProperty(PARAMETER_IMAP_ENABLED, String.valueOf(enabled));
    }

    public boolean isImapEnabled() {
        return getBooleanParameter(PARAMETER_IMAP_ENABLED, DEFAULT_IMAP_ENABLED);
    }

    /**
     * @return the password for the SSL keystore.
     * @since 0.2.10
     */
    public String getSSLKeyStorePassword() {
        return properties.getProperty(PARAMETER_SSL_KEYSTORE_PASSWORD);
    }

    /**
     * Returns the maximum number of email identities to retrieve new emails for at a time.
     */
    public int getMaxConcurIdCheckMail() {
        return getIntParameter(PARAMETER_MAX_CONCURRENT_IDENTITIES_CHECK_MAIL, DEFAULT_MAX_CONCURRENT_IDENTITIES_CHECK_MAIL);
    }

    public void setAutoMailCheckEnabled(boolean enabled) {
        properties.setProperty(PARAMETER_AUTO_MAIL_CHECK, String.valueOf(enabled));
    }

    public boolean isAutoMailCheckEnabled() {
        return getBooleanParameter(PARAMETER_AUTO_MAIL_CHECK, DEFAULT_AUTO_MAIL_CHECK);
    }

    public void setDeliveryCheckEnabled(boolean enabled) {
        properties.setProperty(PARAMETER_DELIVERY_CHECK, String.valueOf(enabled));
    }

    public boolean isDeliveryCheckEnabled() {
        return getBooleanParameter(PARAMETER_DELIVERY_CHECK, DEFAULT_DELIVERY_CHECK);
    }

    public void setMailCheckInterval(int minutes) {
        properties.setProperty(PARAMETER_MAIL_CHECK_INTERVAL, String.valueOf(minutes));
    }

    /**
     * Returns the number of minutes the application should wait before 
     * checking for mail again. This setting only has an effect if
     * automatic mail checking is disabled.
     * @see #isAutoMailCheckEnabled()
     */
    public int getMailCheckInterval() {
        return getIntParameter(PARAMETER_MAIL_CHECK_INTERVAL, DEFAULT_MAIL_CHECK_INTERVAL);
    }

    public void setOutboxCheckInterval(int minutes) {
        properties.setProperty(PARAMETER_OUTBOX_CHECK_INTERVAL, String.valueOf(minutes));
    }

    /**
     * Returns the wait time, in minutes, before processing the outbox folder again.
     * @see i2p.bote.service.OutboxProcessor
     */
    public int getOutboxCheckInterval() {
        return getIntParameter(PARAMETER_OUTBOX_CHECK_INTERVAL, DEFAULT_OUTBOX_CHECK_INTERVAL);
    }

    public void getDeliveryCheckInterval(int minutes) {
        properties.setProperty(PARAMETER_DELIVERY_CHECK_INTERVAL, String.valueOf(minutes));
    }

    /**
     * Returns the wait time, in minutes, between checking the delivery status of sent emails.
     * @see i2p.bote.service.DeliveryChecker
     */
    public int getDeliveryCheckInterval() {
        return getIntParameter(PARAMETER_DELIVERY_CHECK_INTERVAL, DEFAULT_DELIVERY_CHECK_INTERVAL);
    }

    public void setRelaySendPause(int minutes) {
        properties.setProperty(PARAMETER_RELAY_SEND_PAUSE, String.valueOf(minutes));
    }

    /**
     * Returns the number of minutes to wait before processing the relay packet folder again.
     */
    public int getRelaySendPause() {
        return getIntParameter(PARAMETER_RELAY_SEND_PAUSE, DEFAULT_RELAY_SEND_PAUSE);
    }

    /**
     * Controls whether strings that are added to outgoing email, like "Re:" or "Fwd:",
     * are translated or not.<br/>
     * If <code>hideLocale</code> is <code>false</code>, the UI language is used.<br/>
     * If <code>hideLocale</code> is <code>true</code>, the strings are left untranslated
     * (which means they are in English).
     * @param hideLocale
     */
    public void setHideLocale(boolean hideLocale) {
        properties.setProperty(PARAMETER_HIDE_LOCALE, String.valueOf(hideLocale));
    }

    public boolean getHideLocale() {
        return getBooleanParameter(PARAMETER_HIDE_LOCALE, DEFAULT_HIDE_LOCALE);
    }

    /**
     * Controls whether the send time is included in outgoing emails.
     * @param includeSentTime
     */
    public void setIncludeSentTime(boolean includeSentTime) {
        properties.setProperty(PARAMETER_INCLUDE_SENT_TIME, String.valueOf(includeSentTime));
    }

    public boolean getIncludeSentTime() {
        return getBooleanParameter(PARAMETER_INCLUDE_SENT_TIME, DEFAULT_INCLUDE_SENT_TIME);
    }

    public int getMessageIdCacheSize() {
        return getIntParameter(PARAMETER_MESSAGE_ID_CACHE_SIZE, DEFAULT_MESSAGE_ID_CACHE_SIZE);
    }

    /**
     * Returns the number of relay chains that should be used per Relay Request.
     */
    public int getRelayRedundancy() {
        return getIntParameter(PARAMETER_RELAY_REDUNDANCY, DEFAULT_RELAY_REDUNDANCY);
    }

    public void setRelayMinDelay(int minDelay) {
        properties.setProperty(PARAMETER_RELAY_MIN_DELAY, String.valueOf(minDelay));
    }

    /**
     * Returns the minimum amount of time in minutes that a Relay Request is delayed.
     */
    public int getRelayMinDelay() {
        return getIntParameter(PARAMETER_RELAY_MIN_DELAY, DEFAULT_RELAY_MIN_DELAY);
    }

    public void setRelayMaxDelay(int maxDelay) {
        properties.setProperty(PARAMETER_RELAY_MAX_DELAY, String.valueOf(maxDelay));
    }

    /**
     * Returns the maximum amount of time in minutes that a Relay Request is delayed.
     */
    public int getRelayMaxDelay() {
        return getIntParameter(PARAMETER_RELAY_MAX_DELAY, DEFAULT_RELAY_MAX_DELAY);
    }

    public void setNumStoreHops(int numHops) {
        properties.setProperty(PARAMETER_NUM_STORE_HOPS, String.valueOf(numHops));
    }

    /**
     * Returns the number of relays that should be used when sending a DHT store request.
     * @return A non-negative number
     */
    public int getNumStoreHops() {
        return getIntParameter(PARAMETER_NUM_STORE_HOPS, DEFAULT_NUM_STORE_HOPS);
    }

    public void setGatewayDestination(String destination) {
        properties.setProperty(PARAMETER_GATEWAY_DESTINATION, destination);
    }

    public String getGatewayDestination() {
        return properties.getProperty(PARAMETER_GATEWAY_DESTINATION, DEFAULT_GATEWAY_DESTINATION);
    }

    public void setGatewayEnabled(boolean enable) {
        properties.setProperty(PARAMETER_GATEWAY_ENABLED, String.valueOf(enable));
    }

    public boolean isGatewayEnabled() {
        return getBooleanParameter(PARAMETER_GATEWAY_ENABLED, DEFAULT_GATEWAY_ENABLED);
    }

    public void setPasswordCacheDuration(int duration) {
        properties.setProperty(PARAMETER_PASSWORD_CACHE_DURATION, String.valueOf(duration));
    }

    /**
     * Returns the number of minutes the password is kept in memory
     */
    public int getPasswordCacheDuration() {
        return getIntParameter(PARAMETER_PASSWORD_CACHE_DURATION, DEFAULT_PASSWORD_CACHE_DURATION);
    }

    public String getEeproxyHost() {
        return properties.getProperty(PARAMETER_EEPROXY_HOST, DEFAULT_EEPROXY_HOST);
    }

    public int getEeproxyPort() {
        return getIntParameter(PARAMETER_EEPROXY_PORT, DEFAULT_EEPROXY_PORT);
    }

    /**
     * Returns an HTTP URL pointing to the .xpi2p update file.
     */
    public String getUpdateUrl() {
        return properties.getProperty(PARAMETER_UPDATE_URL, DEFAULT_UPDATE_URL);
    }

    /**
     * Returns the number of minutes to wait after checking for a new plugin version.
     */
    public int getUpdateCheckInterval() {
        return getIntParameter(PARAMETER_UPDATE_CHECK_INTERVAL, DEFAULT_UPDATE_CHECK_INTERVAL);
    }

    public void setThemeUrl(String url) {
        properties.setProperty(PARAMETER_THEME, url);
    }

    /**
     * Returns the name of the current UI theme.
     */
    public String getTheme() {
        return properties.getProperty(PARAMETER_THEME, DEFAULT_THEME);
    }

    /**
     * Returns a list of all available UI themes.
     */
    public List<Theme> getThemes() {
        List<Theme> themes = new ArrayList<Theme>();
        themes.addAll(getBuiltInThemes());
        themes.addAll(getExternalThemes());
        return themes;
    }

    /**
     * Returns only the UI themes that are included in the application.
     */
    public List<Theme> getBuiltInThemes() {
        return BUILT_IN_THEMES;
    }

    /**
     * Returns the directory where the application looks for additional UI themes.
     */
    public File getExternalThemeDir() {
        return new File(i2pBoteDir, THEME_SUBDIR);
    }

    private List<Theme> getExternalThemes() {
        File[] dirs = new File(i2pBoteDir, THEME_SUBDIR).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        List<Theme> themes = new ArrayList<Theme>();
        if (dirs != null)
            for (File dir: dirs) {
                String themeId = dir.getName();
                Theme theme = new Theme(themeId, themeId);
                themes.add(theme);
            }
        return themes;
    }

    /**
     * Returns the File that contains the version the I2P-Bote data directory was last
     * successfully migrated to.
     */
    public File getMigrationVersionFile() {
        return new File(i2pBoteDir, MIGRATION_VERSION_FILE);
    }

    private boolean getBooleanParameter(String parameterName, boolean defaultValue) {
        try {
            return Util.getBooleanParameter(properties, parameterName, defaultValue);
        } catch (IllegalArgumentException e) {
            log.warn("getBooleanParameter failed, using default", e);
            return defaultValue;
        }
    }

    private int getIntParameter(String parameterName, int defaultValue) {
        try {
            return Util.getIntParameter(properties, parameterName, defaultValue);
        } catch (NumberFormatException e) {
            log.warn("getIntParameter failed, using default", e);
            return defaultValue;
        }
    }

    /** Simple class that represents a UI theme */
    public static class Theme {
        private String id;
        private String displayName;

        private Theme(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Theme other = (Theme) obj;
            if (id == null) {
                if (other.id != null)
                    return false;
            } else if (!id.equals(other.id))
                return false;
            return true;
        }
    }
}
