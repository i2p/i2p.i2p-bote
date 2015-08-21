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

package i2p.bote.service;

import i2p.bote.Configuration;
import i2p.bote.Util;
import i2p.bote.email.EmailIdentity;
import i2p.bote.email.Identities;
import i2p.bote.fileencryption.PasswordException;
import i2p.bote.folder.EmailPacketFolder;
import i2p.bote.folder.IncompleteEmailFolder;
import i2p.bote.folder.IndexPacketFolder;
import i2p.bote.network.CheckEmailTask;
import i2p.bote.network.DHT;
import i2p.bote.network.I2PSendQueue;
import i2p.bote.network.NetworkStatusSource;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.i2p.util.I2PAppThread;
import net.i2p.util.Log;

public class EmailChecker extends I2PAppThread {
    private Log log = new Log(EmailChecker.class);
    private Identities identities;
    private Configuration configuration;
    private IncompleteEmailFolder incompleteEmailFolder;
    private EmailPacketFolder emailDhtStorageFolder;
    private IndexPacketFolder indexPacketDhtStorageFolder;
    private NetworkStatusSource networkStatusSource;
    private I2PSendQueue sendQueue;
    private DHT dht;
    private RelayPeerManager peerManager;
    private ThreadFactory mailCheckThreadFactory;
    private ExecutorService mailCheckExecutor;
    private Map<EmailIdentity, Future<Boolean>> pendingMailCheckTasks;
    private volatile long lastMailCheckTime;   // the time when the last mail check started (completed or not)
    private volatile long previousMailCheckTime;   // the time when the last completed mail check started
    private long interval;   // in milliseconds
    private volatile boolean newMailReceived;
    
    /**
     * @param identities
     * @param configuration
     * @param incompleteEmailFolder
     * @param emailDhtStorageFolder
     * @param indexPacketDhtStorageFolder
     * @param networkStatusSource
     * @param sendQueue
     * @param dht
     * @param peerManager
     */
    public EmailChecker(Identities identities, Configuration configuration,
            IncompleteEmailFolder incompleteEmailFolder, EmailPacketFolder emailDhtStorageFolder, IndexPacketFolder indexPacketDhtStorageFolder,
            NetworkStatusSource networkStatusSource, I2PSendQueue sendQueue, DHT dht, RelayPeerManager peerManager) {
        super("EmailChecker");
        this.identities = identities;
        this.configuration = configuration;
        this.incompleteEmailFolder = incompleteEmailFolder;
        this.emailDhtStorageFolder = emailDhtStorageFolder;
        this.indexPacketDhtStorageFolder = indexPacketDhtStorageFolder;
        this.networkStatusSource = networkStatusSource;
        this.sendQueue = sendQueue;
        this.dht = dht;
        this.peerManager = peerManager;
        mailCheckThreadFactory = Util.createThreadFactory("ChkEmailTask", CheckEmailTask.THREAD_STACK_SIZE);
        mailCheckExecutor = new ThreadPoolExecutor(
                0,
                configuration.getMaxConcurIdCheckMail(),
                30,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                mailCheckThreadFactory);
        pendingMailCheckTasks = Collections.synchronizedMap(new HashMap<EmailIdentity, Future<Boolean>>());
        interval = configuration.getMailCheckInterval();
        interval = TimeUnit.MINUTES.toMillis(interval);
    }

    public synchronized void checkForMail() throws PasswordException, IOException, GeneralSecurityException {
        if (!isCheckingForMail()) {
            if (identities.size() <= 0)
                log.info("Not checking for mail because no identities are defined.");
            else
                log.info("Checking mail for " + identities.size() + " Email Identities...");
            
            previousMailCheckTime = lastMailCheckTime;
            lastMailCheckTime = System.currentTimeMillis();
            for (EmailIdentity identity : identities.getAll()) {
                if (identity.getIncludeInGlobalCheck()) {
                    checkForMail(identity);
                }
            }
        }
        else
            log.info("Not checking for mail because the last mail check hasn't finished.");
    }

    public synchronized void checkForMail(String key) throws PasswordException, IOException, GeneralSecurityException {
        if (!isCheckingForMail()) {
            previousMailCheckTime = lastMailCheckTime;
            lastMailCheckTime = System.currentTimeMillis();
        }
        EmailIdentity identity = identities.get(key);
        if (identity != null)
            checkForMail(identity);
    }

    public synchronized void checkForMail(EmailIdentity identity) {
        if (!pendingMailCheckTasks.containsKey(identity)) {
            Callable<Boolean> checkMailTask = new CheckEmailTask(identity, dht, peerManager, sendQueue, incompleteEmailFolder, emailDhtStorageFolder, indexPacketDhtStorageFolder);
            Future<Boolean> task = mailCheckExecutor.submit(checkMailTask);
            pendingMailCheckTasks.put(identity, task);
        }
    }

    /**
     * @return true if any EmailIdentities are being checked for mail.
     */
    public synchronized boolean isCheckingForMail() {
        updatePendingTasks();
        return !pendingMailCheckTasks.isEmpty();
    }

    /**
     * @param identity The EmailIdentity to test.
     * @return true if this EmailIdentity is being checked for mail.
     */
    public synchronized boolean isCheckingForMail(EmailIdentity identity) {
        return isCheckingForMail() && pendingMailCheckTasks.containsKey(identity);
    }
    
    /**
     * Returns time at which the last completed email check was started.
     * @return a time value in milliseconds since 1/1/1970, or zero if mail hasn't been checked yet
     */
    public long getLastMailCheckTime() {
        if (isCheckingForMail())
            return previousMailCheckTime;
        else
            return lastMailCheckTime;
    }
    
    /**
     * Returns <code>true</code> if the last call to {@link #checkForMail()} has completed
     * and added new mail to the inbox.</br>
     * If this method returns <code>true</code>, subsequent calls will always return
     * <code>false</code> until {@link #checkForMail()} is executed again.
     */
    public synchronized boolean newMailReceived() {
        if (pendingMailCheckTasks == null)
            return false;
        if (isCheckingForMail())
            return false;

        if (newMailReceived) {
            newMailReceived = false;
            return true;
        }

        return false;
    }

    private synchronized void updatePendingTasks() {
        try {
            synchronized (pendingMailCheckTasks) {
                Iterator<Map.Entry<EmailIdentity, Future<Boolean>>> iter = pendingMailCheckTasks.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<EmailIdentity, Future<Boolean>> entry = iter.next();
                    try {
                        boolean newMailForIdentity = entry.getValue().get(1, TimeUnit.MILLISECONDS);
                        iter.remove();
                        if (newMailForIdentity)
                            newMailReceived = true;
                    } catch (TimeoutException e) {
                        log.debug("CheckEmailTask not finished for Identity " + entry.getKey());
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error while checking whether new mail has arrived.", e);
        }
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                long timeSinceLastCheck = System.currentTimeMillis() - lastMailCheckTime;
                if (!networkStatusSource.isConnected())   // if not connected, use a shorter wait interval
                    TimeUnit.MINUTES.sleep(1);
                else if (timeSinceLastCheck < interval)
                    TimeUnit.MILLISECONDS.sleep(interval - timeSinceLastCheck);
                else {
                    if (configuration.isAutoMailCheckEnabled())
                        try {
                            checkForMail();
                        } catch (PasswordException e) {
                            log.debug("Can't auto-check for email because a password is set.");
                        } catch (IOException e) {
                            log.debug("Can't auto-check for email.", e);
                        } catch (GeneralSecurityException e) {
                            log.debug("Can't auto-check for email.", e);
                        }
                    TimeUnit.MINUTES.sleep(1);
                }
            } catch (InterruptedException e) {
                break;
            } catch (RuntimeException e) {   // catch unexpected exceptions to keep the thread running
                log.error("Exception caught in EmailChecker loop", e);
            }
        }

        mailCheckExecutor.shutdownNow();
        log.debug("EmailChecker interrupted, thread exiting.");
    }
}