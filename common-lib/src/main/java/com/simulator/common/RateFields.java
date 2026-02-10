package com.simulator.common;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RateFields {

    private double bid;
    private double ask;
    private String timestamp; // ISO string formatında saklanacak

    // Default constructor for Jackson
    public RateFields() {}

    public RateFields(double bid, double ask, String timestamp) {
        this.bid = bid;
        this.ask = ask;
        this.timestamp = timestamp;
    }

    // Getters
    public double getBid() {
        return bid;
    }

    public void setBid(double bid) {
        this.bid = bid;
    }

    public double getAsk() {
        return ask;
    }

    public void setAsk(double ask) {
        this.ask = ask;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    // JSON string'e dönüştürme (Redis için)
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }

    // toString metodu loglamalar için
    @Override
    public String toString() {
        return "RateFields [bid=" + bid + ", ask=" + ask + ", timestamp=" + timestamp + "]";
    }
}
