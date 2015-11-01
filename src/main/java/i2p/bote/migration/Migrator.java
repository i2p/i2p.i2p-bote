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

package i2p.bote.migration;

import i2p.bote.Configuration;
import i2p.bote.Util;
import i2p.bote.fileencryption.PasswordCache;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.fileencryption.PasswordHolder;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import net.i2p.util.Log;
import net.i2p.util.VersionComparator;

public class Migrator {
    private Log log = new Log(Migrator.class);
    private Configuration configuration;
    private String currentVersion;
    private String lastMigrationVersion;

    public Migrator(Configuration configuration, String currentVersion) {
        this.configuration = configuration;
        this.currentVersion = currentVersion;
        lastMigrationVersion = getLastSuccessfulMigration();
        log.debug("Last migration was to version <" + lastMigrationVersion + ">. Current version is <" + currentVersion + ">.");
    }

    /**
     * Migrates files that are not encrypted, or encrypted with the default password.
     */
    public void migrateNonPasswordedDataIfNeeded() {
        if (VersionComparator.comp(lastMigrationVersion, currentVersion) >= 0) {
            log.debug("No plaintext migration needed.");
            return;
        }

        try {
            boolean migrationSucceeded = true;

            if (VersionComparator.comp(lastMigrationVersion, "0.2.6") < 0)
                new MigrateTo026().migrateIfNeeded(configuration);
            if (VersionComparator.comp(lastMigrationVersion, "0.2.7") < 0)
                new MigrateTo027().migrateIfNeeded(configuration);

            if (VersionComparator.comp(lastMigrationVersion, "0.2.8") < 0) {
                PasswordCache defaultPasswordHolder = new PasswordCache(configuration);
                try {
                    new MigrateTo028().migrateIfNeeded(configuration, defaultPasswordHolder);
                } catch (PasswordException e) {
                    log.debug("Non-default password in use, deferring migration of encrypted files to after password entry");
                    migrationSucceeded = false;
                }
            }

            if (migrationSucceeded) {
                log.debug("Migration successful, setting last successful migration to <" + currentVersion + ">.");
                setLastSuccessfulMigration(currentVersion);
            }
        }
        catch (Exception e) {
            log.error("Error migrating to the latest version.", e);
        }
    }

    /**
     * Migrates password-protected files. This method assumes it is
     * called after {@link #migrateNonPasswordedDataIfNeeded()}.
     * @param passwordHolder
     */
    public void migratePasswordedDataIfNeeded(PasswordHolder passwordHolder) {
        if (VersionComparator.comp(lastMigrationVersion, currentVersion) >= 0) {
            log.debug("No encrypted-file migration needed.");
            return;
        }

        try {
            if (VersionComparator.comp(lastMigrationVersion, "0.2.8") < 0)
                new MigrateTo028().migrateIfNeeded(configuration, passwordHolder);

            // we're assuming migrateNonPasswordedDataIfNeeded() ran already
            log.debug("Encrypted-file migration successful, setting last successful migration to <" + currentVersion + ">.");
            setLastSuccessfulMigration(currentVersion);
        }
        catch (Exception e) {
            log.error("Error migrating to the latest version.", e);
        }
    }

    /**
     * Returns the version to which the I2P-Bote data directory was last migrated to.
     * If there has never been a migration, zero is returned.
     */
    private String getLastSuccessfulMigration() {
        File versionFile = configuration.getMigrationVersionFile();
        if (!versionFile.exists())
            return "0";

        List<String> lines = Util.readLines(versionFile);
        if (lines.isEmpty())
            return "0";

        return lines.get(0);
    }

    /**
     * Writes the version to which the I2P-Bote data directory was last migrated to,
     * to a file.
     * @param version
     * @throws IOException
     */
    private void setLastSuccessfulMigration(String version) throws IOException {
        File versionFile = configuration.getMigrationVersionFile();
        DataOutputStream outputStream = null;
        try {
            outputStream = new DataOutputStream(new FileOutputStream(versionFile));
            outputStream.writeBytes(version);
        }
        finally {
            if (outputStream != null)
                outputStream.close();
        }
    }
}