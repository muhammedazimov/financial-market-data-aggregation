package com.simulator.common;

// Simple listener interface that collectors will call.
// Coordinator will implement this to receive data callbacks.
public interface RateListener {

    // Fired when a platform connection is established
    void onConnect(String platform, boolean status);

    // Fired when a platform connection is closed
    void onDisconnect(String platform, boolean status);

    // Fired the first time a subscribed rate arrives (optional)
    void onRateAvailable(String subscriberId, String platform, String rateName, RateFields fields);

    // Fired for each subsequent update of a subscribed rate
    void onRateUpdate(String subscriberId, String platform, String rateName, RateFields fields);

    // Fired for status / heartbeat messages (optional)
    void onRateStatus(String platform, String rateName, String status);
}
