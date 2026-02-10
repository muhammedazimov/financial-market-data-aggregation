package com.simulator.mainapp;

public interface IRateCollector {
    void connect(String platformName, String user, String password);
    void disconnect(String platformName, String user, String password);
    void subscribe(String platformName, String rateName);
    void unsubscribe(String platformName, String rateName);
}
