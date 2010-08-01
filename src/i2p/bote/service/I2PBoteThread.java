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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.i2p.util.I2PAppThread;
import net.i2p.util.Log;

/**
 * A base class for background threads.<br/>
 * The {@link #run()} method is final; subclasses override
 * {@link #doStep()} and optionally the methods {@link #postStartup()}
 * and {@link #preShutdown()}.
 */
public abstract class I2PBoteThread extends I2PAppThread {
    private Log log = new Log(I2PBoteThread.class);
    private CountDownLatch shutdownSignal;
    
    protected I2PBoteThread(String name) {
        super(name);
        shutdownSignal = new CountDownLatch(1);
    }
    
    public void requestShutdown() {
        shutdownSignal.countDown();
    }
    
    public boolean awaitShutdownRequest(long timeout, TimeUnit unit) {
        try {
            return shutdownSignal.await(timeout, unit);
        } catch (InterruptedException e) {
            log.error("Interrupted in thread <" + getName() + ">", e);
            shutdownSignal.countDown();
            return true;
        }
    }
    
    public boolean shutdownRequested() {
        return awaitShutdownRequest(0, TimeUnit.SECONDS);
    }
    
    /**
     * Executes {@link #doStep()} in a loop until {@link #requestShutdown()} is called.
     * There is no pause between the <code>doStep</code> calls.<br/>
     * Before the first invocation of <code>doStep</code>, {@link #postStartup()} is called.</br>
     * After the last invocation of <code>doStep</code>, {@link #preShutdown()} is called.</br>
     */
    @Override
    public final void run() {
        postStartup();
        
        while (true) {
            if (shutdownRequested())
                break;
            try {
                doStep();
            }
            catch (InterruptedException e) {
                log.error("Interrupted in thread <" + getName() + ">", e);
                break;
            }
            catch (Throwable t) {
                log.error("An exception occurred in thread " + getName(), t);
            }
        }
        
        try {
            preShutdown();
        }
        catch (Throwable t) {
            log.error("An exception occurred in thread " + getName(), t);
        }
            
        log.info("Thread " + getName() + " exiting.");
    }
    
    /**
     * This method is called after {@link #postStartup()} and continues
     * to be called every time it returns, until {@link #requestShutdown()}
     * is invoked.<br/>
     * Because the invocations happen without interruption, all implementations
     * of this method call <code>Thread.sleep</code>.
     * @throws InterruptedException
     * @see #run()
     */
    protected abstract void doStep() throws InterruptedException;
    
    /**
     * @see #run()
     */
    protected void postStartup() { }
    
    /**
     * @see #run()
     */
    protected void preShutdown() throws InterruptedException { }
}