package com.simulator.mainapp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.simulator.common.RateFields;
import com.simulator.common.RateListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Coordinator implements RateListener so collectors can notify it
public class Coordinator implements RateListener {

    private static final Logger logger = LogManager.getLogger(Coordinator.class);

    private final RedisClient redis; // replace with your actual Redis client
    private final Map<String, Map<String, RateFields>> cache = new ConcurrentHashMap<>();
    private final KafkaPublisher kafkaPublisher;

    public Coordinator(RedisClient redis, KafkaPublisher kafkaPublisher) {
        this.redis = redis;
        this.kafkaPublisher = kafkaPublisher;
    }

    @Override
    public void onConnect(String platform, boolean status) {
        logger.info("[Coordinator] onConnect platform={} status={}", platform, status);
    }

    @Override
    public void onDisconnect(String platform, boolean status) {
        logger.info("[Coordinator] onDisconnect platform={} status={}", platform, status);
    }

    @Override
    public void onRateAvailable(String subscriberId, String platform, String rateName, RateFields fields) {
        logger.info("[Coordinator] onRateAvailable rate={}", rateName);
        // forward to update logic
        onRateUpdate(subscriberId, platform, rateName, fields);
    }

    @Override
    public void onRateUpdate(String subscriberId, String platform, String rateName, RateFields fields) {
        try {
            // Coordinator gerçekten tick alıyor mu kontrol logu
            logger.debug("[Coordinator] onRateUpdate sub={} pf={} rate={} bid={} ask={} ts={}",
                    subscriberId, platform, rateName,
                    fields.getBid(), fields.getAsk(), fields.getTimestamp());

            // ----------------------------
            // 1) RAW veriyi Redis'e kaydet
            // ----------------------------
            String rawKey = "raw:" + subscriberId + ":" + rateName;
            String rawVal = rateName + "|" + fields.getBid() + "|" + fields.getAsk() + "|" + fields.getTimestamp();
            redis.set(rawKey, rawVal);
            logger.info("[Redis] RAW saved -> {} = {}", rawKey, rawVal);

            // Kafka'ya RAW publish et
            String msg = rateName + "|" + fields.getBid() + "|" + fields.getAsk() + "|" + fields.getTimestamp();
            kafkaPublisher.send(rateName, msg);
            logger.info("[Kafka] Published -> {}", msg);

            // ----------------------------
            // 2) Cache'e ekle (hesaplama için)
            // ----------------------------
            cache.computeIfAbsent(subscriberId, k -> new ConcurrentHashMap<>()).put(rateName, fields);

            // ----------------------------
            // 3) UniversalRateCalculator ile hesapla
            // ----------------------------
            Map<String, RateFields> calculated = UniversalRateCalculator.calculate(cache.get(subscriberId));

            // ----------------------------
            // 4) CALC sonuçlarını Redis ve Kafka'ya kaydet
            // ----------------------------
            for (Map.Entry<String, RateFields> e : calculated.entrySet()) {
                String calcKey = "calc:" + subscriberId + ":" + e.getKey();
                String calcVal = e.getKey() + "|" +
                        e.getValue().getBid() + "|" +
                        e.getValue().getAsk() + "|" +
                        e.getValue().getTimestamp();

                redis.set(calcKey, calcVal);
                kafkaPublisher.send(calcKey, calcVal);

                logger.info("[Redis] CALC saved -> {} = {}", calcKey, calcVal);
                logger.info("[Kafka] CALC Published -> {} = {}", calcKey, calcVal);
            }

        } catch (Exception e) {
            logger.error("[Coordinator] Error in onRateUpdate for rate={}", rateName, e);
        }
    }

    @Override
    public void onRateStatus(String platform, String rateName, String status) {
        logger.info("[Coordinator] onRateStatus platform={} rate={} status={}", platform, rateName, status);
    }
}
