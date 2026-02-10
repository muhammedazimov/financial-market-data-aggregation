package com.simulator.mainapp;

import com.simulator.common.IRateCollector;
import com.simulator.common.RateFields;
import com.simulator.common.RateListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PF1RateCollector implements IRateCollector {

    private static final Logger logger = LogManager.getLogger(PF1RateCollector.class);

    private String host = System.getenv("PF1_HOST") != null ? System.getenv("PF1_HOST") : "localhost";
    private int port = 5001;
    private String subscriberId;
    private RateListener listener;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private final ExecutorService readerExecutor = Executors.newSingleThreadExecutor();

    public PF1RateCollector() {
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
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            logger.info("[PF1Collector] Connected to {}:{}", host, port);

            if (listener != null)
                listener.onConnect(platformName, true);

            // ayrı thread ile okuma başlat
            readerExecutor.submit(() -> readLoop(platformName));

        } catch (IOException e) {
            logger.error("[PF1Collector] Failed to connect to {}:{}", host, port, e);
            if (listener != null)
                listener.onConnect(platformName, false);
        }
    }

    private void readLoop(String platformName) {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                logger.debug("[PF1Collector] Received line: {}", line);

                if (line.startsWith("Subscribed") || line.startsWith("Unsubscribed") || line.startsWith("ERROR")) {
                    logger.info("[PF1Collector] Control message: {}", line);
                    continue;
                }

                String[] parts = line.split("\\|");
                if (parts.length >= 4) {
                    String rateName = parts[0];
                    double bid = parseValue(parts[1]);
                    double ask = parseValue(parts[2]);
                    String timestamp = parseTimestamp(parts[3]);

                    RateFields fields = new RateFields(bid, ask, timestamp);
                    logger.info("[PF1Collector] Rate update sub={} rate={} bid={} ask={} ts={}",
                            subscriberId, rateName, bid, ask, timestamp);

                    if (listener != null) {
                        listener.onRateUpdate(subscriberId, platformName, rateName, fields);
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("[PF1Collector] Connection closed for subscriber={}", subscriberId);
        }
    }

    private double parseValue(String part) {
        try {
            String[] subParts = part.split(":");
            String valueStr = subParts[subParts.length - 1].replace(",", ".");
            return Double.parseDouble(valueStr);
        } catch (Exception e) {
            logger.error("[PF1Collector] Parse error for part={} : {}", part, e.getMessage());
            return 0.0;
        }
    }

    private String parseTimestamp(String field) {
        try {
            String[] p = field.split(":");
            return p[2];
        } catch (Exception e) {
            logger.error("[PF1Collector] Timestamp parse error for field={} : {}", field, e.getMessage());
            return "";
        }
    }

    @Override
    public void subscribe(String platformName, String rateName) {
        if (out != null) {
            out.println("subscribe|" + rateName);
            logger.info("[PF1Collector] Sent subscribe request for rate={}", rateName);
        }
    }

    @Override
    public void unsubscribe(String platformName, String rateName) {
        if (out != null) {
            out.println("unsubscribe|" + rateName);
            logger.info("[PF1Collector] Sent unsubscribe request for rate={}", rateName);
        }
    }

    @Override
    public void disconnect(String platformName, String user, String password) {
        try {
            if (socket != null)
                socket.close();
            readerExecutor.shutdownNow();
            logger.info("[PF1Collector] Disconnected subscriber={}", subscriberId);
            if (listener != null)
                listener.onDisconnect(platformName, true);
        } catch (IOException e) {
            logger.error("[PF1Collector] Error during disconnect for subscriber={}", subscriberId, e);
        }
    }
}
