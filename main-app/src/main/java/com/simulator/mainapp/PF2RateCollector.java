package com.simulator.mainapp;

import com.simulator.common.IRateCollector;
import com.simulator.common.RateFields;
import com.simulator.common.RateListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

// PF2 platformu için Collector (REST API üzerinden polling yapar)
public class PF2RateCollector implements IRateCollector, Runnable {

    private static final Logger logger = LogManager.getLogger(PF2RateCollector.class);

    private RateListener listener;
    private String subscriberId;

    // PF2 polls rates, so it needs to know what rates to poll.
    // Since subscribe() method is void and meant to send a command,
    // we need to store the subscriptions here.
    private final List<String> subscribedRates = new ArrayList<>();

    private volatile boolean running = false;
    private Thread pollingThread;

    public PF2RateCollector() {
    }

    @Override
    public void setListener(RateListener listener) {
        this.listener = listener;
    }

    @Override
    public void setSubscriberId(String subscriberId) {
        this.subscriberId = subscriberId;
    }

    @Override
    public void connect(String platformName, String userId, String password) {
        running = true;
        if (listener != null)
            listener.onConnect(platformName, true);
        logger.info("[PF2Collector] Connected (REST polling mode) user={} platform={}", userId, platformName);

        // Start polling thread
        pollingThread = new Thread(this);
        pollingThread.start();
    }

    @Override
    public void disconnect(String platformName, String userId, String password) {
        running = false;
        if (listener != null)
            listener.onDisconnect(platformName, true);
        logger.info("[PF2Collector] Disconnected user={} platform={}", userId, platformName);
    }

    @Override
    public void subscribe(String platformName, String rateName) {
        if (!subscribedRates.contains(rateName)) {
            subscribedRates.add(rateName);
            logger.info("[PF2Collector] Subscribed to {}", rateName);
        }
    }

    @Override
    public void unsubscribe(String platformName, String rateName) {
        subscribedRates.remove(rateName);
        logger.info("[PF2Collector] Unsubscribed from {}", rateName);
    }

    @Override
    public void run() {
        try {
            while (running) {
                // Poll for each subscribed rate
                // To avoid ConcurrentModificationException if subscribe is called during
                // iteration
                List<String> rates = new ArrayList<>(subscribedRates);

                for (String coreRate : rates) {
                    pollRate(coreRate);
                }

                Thread.sleep(2000);
            }
        } catch (Exception e) {
            logger.error("[PF2Collector] Error in run loop", e);
            if (listener != null)
                listener.onDisconnect("PF2", false);
        }
    }

    private void pollRate(String rateName) {
        try {
            String host = System.getenv("PF2_HOST") != null ? System.getenv("PF2_HOST") : "localhost";
            String urlStr = "http://" + host + ":8080/api/rates/" + rateName;
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);

            if (conn.getResponseCode() != 200) {
                return;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null)
                sb.append(line);
            br.close();

            String json = sb.toString().trim();
            if (json.contains("bid") && json.contains("ask")) {
                parseAndNotify(json);
            }

        } catch (Exception e) {
            logger.debug("[PF2Collector] Failed to poll rate={}: {}", rateName, e.getMessage());
        }
    }

    private void parseAndNotify(String json) {
        try {
            String rateName = json.split("\"rateName\":\"")[1].split("\"")[0];
            String bidStr = json.split("\"bid\":")[1].split(",")[0].replaceAll("[^0-9.\\-]", "").trim();
            String askStr = json.split("\"ask\":")[1].split(",")[0].replaceAll("[^0-9.\\-]", "").trim();
            String ts = json.split("\"timestamp\":\"")[1].split("\"")[0].trim();

            double bid = Double.parseDouble(bidStr);
            double ask = Double.parseDouble(askStr);

            logger.info("[PF2Collector] Tick rate={} bid={} ask={} ts={}", rateName, bid, ask, ts);

            if (listener != null) {
                RateFields fields = new RateFields(bid, ask, ts);
                listener.onRateUpdate(subscriberId, "PF2", rateName, fields);
            }
        } catch (Exception e) {
            logger.error("[PF2Collector] JSON parse error", e);
        }
    }
}
