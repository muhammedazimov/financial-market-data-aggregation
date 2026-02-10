package com.simulator.pf1simulator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class TcpSimulatorServer {

    private static final Logger logger = LogManager.getLogger(TcpSimulatorServer.class);

    private static final int PORT = 5001;

    private static final Map<String, Double[]> BASE_RATES = Map.ofEntries(
            Map.entry("PF1_USDTRY", new Double[]{40.5465, 40.5483}),
            Map.entry("PF1_EURTRY", new Double[]{47.3141, 47.3212}),
            Map.entry("PF1_GBPTRY", new Double[]{54.2783, 54.5503}),
            Map.entry("PF1_CADTRY", new Double[]{29.4543, 29.6020}),
            Map.entry("PF1_CHFTRY", new Double[]{50.4509, 50.7037}),
            Map.entry("PF1_SARTRY", new Double[]{10.7749, 10.8289}),
            Map.entry("PF1_JPYTRY", new Double[]{0.2729, 0.2743}),
            Map.entry("PF1_AUDTRY", new Double[]{26.3853, 26.5176}),
            Map.entry("PF1_NOKTRY", new Double[]{3.9718, 3.9917}),
            Map.entry("PF1_DKKTRY", new Double[]{6.3116, 6.3433}),
            Map.entry("PF1_SEKTRY", new Double[]{4.2226, 4.2438}),
            Map.entry("PF1_RUBTRY", new Double[]{0.5064, 0.5090})
    );

    private static final int SEND_INTERVAL_SECONDS = 3;

    private final Map<Socket, Set<String>> clientSubscriptions = new ConcurrentHashMap<>();

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        logger.info("[PF1Simulator] TCP Simulator started on port {}", PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            logger.info("[PF1Simulator] Client connected: {}", clientSocket.getInetAddress());

            clientSubscriptions.put(clientSocket, ConcurrentHashMap.newKeySet());
            new Thread(() -> handleClient(clientSocket)).start();
        }
    }

    private void handleClient(Socket clientSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

            // *** Data push kısmı ***
            scheduler.scheduleAtFixedRate(() -> {
                Set<String> subscriptions = clientSubscriptions.get(clientSocket);
                if (subscriptions == null || subscriptions.isEmpty()) return;

                for (String rateName : subscriptions) {
                    Double[] base = BASE_RATES.get(rateName);

                    if (base != null) {
                        double bid = random(base[0] - 0.05, base[0] + 0.05);
                        double ask = random(base[1] - 0.05, base[1] + 0.05);
                        String message = formatMessage(rateName, bid, ask);
                        out.println(message);
                        out.flush();
                        logger.debug("[PF1Simulator] Sent tick rate={} bid={} ask={}", rateName, bid, ask);
                        sleep(1000);
                    } else {
                        String reversedRate = reverseRate(rateName);
                        Double[] reversedBase = BASE_RATES.get(reversedRate);
                        if (reversedBase != null) {
                            double reversedBid = 1.0 / (reversedBase[1] + 0.05);
                            double reversedAsk = 1.0 / (reversedBase[0] - 0.05);
                            String message = formatMessage(rateName, reversedBid, reversedAsk);
                            out.println(message);
                            out.flush();
                            logger.debug("[PF1Simulator] Sent reversed tick rate={} bid={} ask={}", rateName, reversedBid, reversedAsk);
                            sleep(1000);
                            continue;
                        }

                        String code = rateName.substring(4).toUpperCase();
                        if (code.length() == 6) {
                            String baseCurrency = code.substring(0, 3);
                            String quoteCurrency = code.substring(3, 6);

                            String baseToTry = "PF1_" + baseCurrency + "TRY";
                            String quoteToTry = "PF1_" + quoteCurrency + "TRY";
                            Double[] baseToTryRate = BASE_RATES.get(baseToTry);
                            Double[] quoteToTryRate = BASE_RATES.get(quoteToTry);

                            if (baseToTryRate != null && quoteToTryRate != null) {
                                double baseBid = random(baseToTryRate[0] - 0.05, baseToTryRate[0] + 0.05);
                                double baseAsk = random(baseToTryRate[1] - 0.05, baseToTryRate[1] + 0.05);
                                double quoteBid = random(quoteToTryRate[0] - 0.05, quoteToTryRate[0] + 0.05);
                                double quoteAsk = random(quoteToTryRate[1] - 0.05, quoteToTryRate[1] + 0.05);

                                double crossBid = baseBid / quoteAsk;
                                double crossAsk = baseAsk / quoteBid;
                                String message = formatMessage(rateName, round(crossBid), round(crossAsk));
                                out.println(message);
                                out.flush();
                                logger.debug("[PF1Simulator] Sent cross tick rate={} bid={} ask={}", rateName, crossBid, crossAsk);
                                sleep(1000);
                                continue;
                            }
                        }
                    }
                }
            }, 0, SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);

            // *** Client mesajlarını ayrı thread ile oku ***
            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        logger.debug("[PF1Simulator] Received from client: {}", line);

                        if (line.startsWith("subscribe|")) {
                            String rate = line.substring("subscribe|".length()).trim().toUpperCase();

                            boolean directExists = BASE_RATES.containsKey(rate);
                            boolean reversedExists = BASE_RATES.containsKey(reverseRate(rate));
                            boolean crossExists = false;

                            String code = rate.length() >= 10 ? rate.substring(4).toUpperCase() : "";
                            if (code.length() == 6) {
                                String baseCurrency = code.substring(0, 3);
                                String quoteCurrency = code.substring(3, 6);
                                String baseToTry = "PF1_" + baseCurrency + "TRY";
                                String quoteToTry = "PF1_" + quoteCurrency + "TRY";
                                crossExists = BASE_RATES.containsKey(baseToTry) && BASE_RATES.containsKey(quoteToTry);
                            }

                            if (directExists || reversedExists || crossExists) {
                                clientSubscriptions.get(clientSocket).add(rate);
                                out.println("Subscribed to " + rate);
                                logger.info("[PF1Simulator] Client subscribed to {}", rate);
                            } else {
                                out.println("ERROR|Rate data not found for " + rate);
                                logger.warn("[PF1Simulator] Subscription failed, rate={} not found", rate);
                            }
                        } else if (line.startsWith("unsubscribe|")) {
                            String rate = line.substring("unsubscribe|".length()).trim();
                            clientSubscriptions.get(clientSocket).remove(rate);
                            out.println("Unsubscribed from " + rate);
                            logger.info("[PF1Simulator] Client unsubscribed from {}", rate);
                        } else {
                            out.println("ERROR|Unknown command");
                            logger.warn("[PF1Simulator] Unknown command received: {}", line);
                        }
                    }
                } catch (IOException e) {
                    logger.warn("[PF1Simulator] Client disconnected unexpectedly");
                } finally {
                    scheduler.shutdown();
                    clientSubscriptions.remove(clientSocket);
                    try { clientSocket.close(); } catch (IOException ignored) {}
                }
            }).start();

        } catch (IOException e) {
            logger.error("[PF1Simulator] Error handling client", e);
            clientSubscriptions.remove(clientSocket);
        }
    }

    private String reverseRate(String rate) {
        if (rate.length() != 10) return rate;
        String code = rate.substring(4).toUpperCase();
        String reversed = code.substring(3) + code.substring(0, 3);
        return "PF1_" + reversed;
    }

    private String formatMessage(String rateName, double bid, double ask) {
        return String.format("%s|22:number:%.5f|25:number:%.5f|5:timestamp:%s",
                rateName, round(bid), round(ask), LocalDateTime.now().toString());
    }

    private double random(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    private double round(double val) {
        return Math.round(val * 100000.0) / 100000.0;
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    public static void main(String[] args) throws IOException {
        new TcpSimulatorServer().start();
    }
}
