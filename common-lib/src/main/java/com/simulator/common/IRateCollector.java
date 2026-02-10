package com.simulator.common;

public interface IRateCollector {
    void setListener(RateListener listener);
    void setSubscriberId(String subscriberId);
    void connect(String platformName, String user, String password);
    void disconnect(String platformName, String user, String password);
    void subscribe(String platformName, String rateName);
    void unsubscribe(String platformName, String rateName);
}
