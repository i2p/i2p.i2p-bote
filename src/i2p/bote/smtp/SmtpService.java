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

package i2p.bote.smtp;

import i2p.bote.Configuration;
import i2p.bote.MailSender;
import i2p.bote.email.Email;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.fileencryption.PasswordVerifier;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.List;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import net.i2p.data.DataFormatException;
import net.i2p.util.Log;

import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.RejectException;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.auth.EasyAuthenticationHandlerFactory;
import org.subethamail.smtp.auth.LoginFailedException;
import org.subethamail.smtp.auth.UsernamePasswordValidator;
import org.subethamail.smtp.server.SMTPServer;

/**
 * IMAP implementation for I2P-Bote using <a href="http://code.google.com/p/subethasmtp/">
 * SubEtha SMTP</a>.
 */
public class SmtpService extends SMTPServer {
    private final static String SMTP_USER = "bote";

    /**
     * 
     * @param configuration
     * @param passwordVerifier
     * @param mailSender
     * @throws UnknownHostException if the listen address cannot be resolved
     */
    public SmtpService(Configuration configuration, PasswordVerifier passwordVerifier, MailSender mailSender) throws UnknownHostException {
        super(new HandlerFactory(mailSender));
        setBindAddress(InetAddress.getByName(configuration.getSmtpAddress()));
        setPort(configuration.getSmtpPort());
        setAuthenticationHandlerFactory(new EasyAuthenticationHandlerFactory(new Validator(passwordVerifier)));
    }
    
    /** Factory class that creates <code>MessageHandler</code>s. */
    private static class HandlerFactory implements MessageHandlerFactory {
        MailSender mailSender;
        
        HandlerFactory(MailSender mailSender) {
            this.mailSender = mailSender;
        }
        
        /** Creates a <code>MessageHandler</code> that sends an email via I2P-Bote. */
        @Override
        public MessageHandler create(MessageContext ctx) {
            return new MessageHandler() {
                
                /** Validates the sender address. */
                @Override
                public void from(String from) throws RejectException {
                    try {
                        from = removeBoteSuffix(from);
                        Email.checkSender(from);
                    } catch(AddressException e) {
                        throw new RejectException();
                    }
                }
                
                /** Validates recipient addresses. */
                @Override
                public void recipient(String recipient) throws RejectException {
                    try {
                        recipient = removeBoteSuffix(recipient);
                        Email.checkRecipient(recipient);
                    }
                    catch(AddressException e) {
                        throw new RejectException();
                    }
                }
                
                /** Receives the email data from the client and queues it for sending. */
                @Override
                public void data(InputStream data) throws RejectException, TooMuchDataException, IOException
                {
                    try {
                        Email email = new Email(data, false);
                        
                        // remove @bote suffixes from email destinations
                        List<Header> addressHeaders = email.getAllAddressHeaders();
                        for (Header header: addressHeaders)
                            email.removeHeader(header.getName());
                        for (Header header: addressHeaders) {
                            String address = header.getValue();
                            address = removeBoteSuffix(address);
                            email.addHeader(header.getName(), address);
                        }
                        
                        mailSender.sendEmail(email);
                    } catch(MessagingException e) {
                        throw new IOException(e);
                    } catch (IOException e) {
                        throw e;
                    } catch (DataFormatException e) {
                        throw new RejectException(e.getLocalizedMessage());
                    } catch (PasswordException e) {
                        throw new RejectException(e.getLocalizedMessage());
                    } catch (GeneralSecurityException e) {
                        throw new IOException(e);
                    }
                }
                
                /**
                 * Removes the "@bote" suffix which an email destination may have
                 * added at the end so the email client accepts it.
                 */
                private String removeBoteSuffix(String address) {
                    if (address.endsWith("@bote"))
                        address = address.substring(0, address.indexOf("@bote"));
                    else if (address.endsWith("@bote>"))
                        address = address.substring(0, address.indexOf("@bote")) + ">";
                    return address;
                }
                
                @Override
                public void done()
                {
                }
            };
        }
    }
    
    /**
     * Checks a username against <code>SMTP_USER</code>, and the password
     * against the I2P-Bote password.
     */
    private static class Validator implements UsernamePasswordValidator {
        PasswordVerifier passwordVerifier;
        
        Validator(PasswordVerifier passwordVerifier) {
            this.passwordVerifier = passwordVerifier;
        }
        
        @Override
        public void login(String username, String password) throws LoginFailedException {
            if (!SMTP_USER.equals(username))
                throw new LoginFailedException();
            
            byte[] passwordBytes = password.toString().getBytes();
            try {
                passwordVerifier.tryPassword(passwordBytes);
            } catch (PasswordException e) {
                throw new LoginFailedException();
            } catch (Exception e) {
                new Log(SmtpService.class).error("Can't check password", e);
                throw new LoginFailedException("Can't check password: " + e.getLocalizedMessage());
            }
        }
    }
}