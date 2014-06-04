package net.i2p.android.router.service;

import net.i2p.android.router.service.IRouterStateCallback;

/**
 * An interface for determining the state of the I2P RouterService.
 */
interface IRouterState {

    /**
     * This allows I2P to inform on state changes.
     */
    void registerCallback(IRouterStateCallback cb);

    /**
     * Remove registered callback interface.
     */
    void unregisterCallback(IRouterStateCallback cb);

    /**
     * Determines whether the RouterService has been started. If it hasn't, no
     * state changes will ever occur from this RouterService instance, and the
     * client should unbind and inform the user that the I2P router is not
     * running (and optionally send a net.i2p.android.router.START_I2P Intent).
     */
    boolean isStarted();

    /**
    * Get the state of the I2P router
    **/
    String getState();

}
