package net.i2p.android.router.service;

/**
 * Callback interface used to send synchronous notifications of the current
 * RouterService state back to registered clients. Note that this is a
 * one-way interface so the server does not block waiting for the client.
 */
oneway interface IRouterStateCallback {
    /**
     * Called when the state of the I2P router changes
     */
    void stateChanged(String newState);
}
