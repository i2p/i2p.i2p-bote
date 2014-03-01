package i2p.bote.smtp;

import java.net.UnknownHostException;

import i2p.bote.Configuration;
import i2p.bote.MailSender;
import i2p.bote.fileencryption.PasswordVerifier;

/**
 * Stubbed-out SmtpService
 */
public class SmtpService {
    public SmtpService(Configuration configuration, PasswordVerifier passwordVerifier, MailSender mailSender) throws UnknownHostException {
    }

    public boolean isRunning() {
        return false;
    }

    public void start() {
    }

    public void stop() {
    }
}
