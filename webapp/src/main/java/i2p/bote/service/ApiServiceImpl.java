/**
 * Copyright (C) 2016 str4d@mail.i2p
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

package i2p.bote.service;

import i2p.bote.Configuration;
import i2p.bote.MailSender;
import i2p.bote.fileencryption.PasswordVerifier;
import i2p.bote.folder.EmailFolderManager;
import i2p.bote.imap.ImapService;
import i2p.bote.smtp.SmtpService;

import java.net.UnknownHostException;

import net.i2p.util.Log;

import org.apache.commons.configuration.ConfigurationException;

public class ApiServiceImpl implements ApiService {
    private Log log = new Log(ApiServiceImpl.class);
    private ImapService imapService;
    private SmtpService smtpService;

    private Configuration configuration;
    private EmailFolderManager folderManager;
    private MailSender mailSender;
    private PasswordVerifier passwordVerifier;

    public ApiServiceImpl(Configuration configuration,
                          EmailFolderManager folderManager,
                          MailSender mailSender,
                          PasswordVerifier passwordVerifier) {
        this.configuration = configuration;
        this.folderManager = folderManager;
        this.mailSender = mailSender;
        this.passwordVerifier = passwordVerifier;
    }

    public void start(int type) {
        switch (type) {
        case IMAP:
            startImap();
            break;
        case SMTP:
            startSmtp();
            break;
        }
    }

    public void stop(int type) {
        switch (type) {
        case IMAP:
            stopImap();
            break;
        case SMTP:
            stopSmtp();
            break;
        }
    }

    public void stopAll() {
        stopImap();
        stopSmtp();
    }

    private void startImap() {
        if (imapService != null && imapService.isStarted())
            return;

        try {
            imapService = ImapService.create(configuration, passwordVerifier, folderManager);
            if (!imapService.start())
                log.error("IMAP service failed to start.");
        } catch (ConfigurationException e) {
            log.error("IMAP service failed to start.", e);
        }
    }

    private void stopImap() {
        if (imapService == null || !imapService.isStarted())
            return;

        if (!imapService.stop())
            log.error("IMAP service failed to stop");
    }

    private void startSmtp() {
        if (smtpService !=null && smtpService.isRunning())
            return;

        try {
            smtpService = new SmtpService(configuration, passwordVerifier, mailSender);
            smtpService.start();
        } catch (UnknownHostException e) {
            log.error("SMTP service failed to start.");
        }
    }

    private void stopSmtp() {
        if (smtpService != null && smtpService.isRunning())
            smtpService.stop();
    }

    public void printRunningThreads() {
        if (imapService!=null && imapService.isStarted())
            log.debug("IMAP service still running");
        if (smtpService!=null && smtpService.isRunning())
            log.debug("SMTP service still running");
    }
}
