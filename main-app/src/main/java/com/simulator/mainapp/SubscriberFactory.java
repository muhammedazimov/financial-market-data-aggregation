package com.simulator.mainapp;

import com.simulator.common.IRateCollector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class SubscriberFactory {

    private static final Logger logger = LogManager.getLogger(SubscriberFactory.class);

    private final Coordinator coordinator; // Verileri yönetecek ana sınıf
    private final Properties props; // Konfigürasyon dosyasından okunan değerler

    // Yapıcı metod -> config.properties dosyasını oku
    public SubscriberFactory(Coordinator coordinator, String configPath) throws IOException {
        this.coordinator = coordinator;
        this.props = new Properties();
        try (FileInputStream fis = new FileInputStream(configPath)) {
            this.props.load(fis);
        }
        logger.info("[SubscriberFactory] Loaded configuration from {}", configPath);
    }

    /**
     * config.properties içindeki subscriber bilgilerini okuyup hepsini başlatır
     */
    public void startAllSubscribers() {
        int count = Integer.parseInt(props.getProperty("subscriber.count"));
        logger.info("[SubscriberFactory] Starting {} subscribers", count);

        // Tüm subscriber'ları sırayla başlat
        for (int i = 1; i <= count; i++) {
            String subscriberId = "subscriber" + i;

            // Config dosyasından subscriber bilgilerini al
            String platform = props.getProperty("subscriber." + i + ".platform").trim().toUpperCase();
            String className = props.getProperty("subscriber." + i + ".class");
            String user = props.getProperty("subscriber." + i + ".user").trim();
            String pass = props.getProperty("subscriber." + i + ".password").trim();
            String[] ratesArr = props.getProperty("subscriber." + i + ".rates").split(",");
            List<String> rateList = Arrays.asList(ratesArr);

            logger.info("[SubscriberFactory] Creating subscriber={} platform={} class={}", subscriberId, platform,
                    className);

            try {
                // Dinamik yükleme
                Class<?> clazz = Class.forName(className);
                Object instance = clazz.getDeclaredConstructor().newInstance();

                if (instance instanceof IRateCollector) {
                    IRateCollector collector = (IRateCollector) instance;

                    // Set dependencies
                    collector.setListener(coordinator);
                    collector.setSubscriberId(subscriberId);

                    // Connect
                    collector.connect(platform, user, pass);

                    // Subscribe
                    for (String rate : rateList) {
                        collector.subscribe(platform, rate);
                        logger.info("[SubscriberFactory] Subscriber={} subscribed to rate={}", subscriberId, rate);
                    }

                } else {
                    logger.error("[SubscriberFactory] Class {} does not implement IRateCollector", className);
                }

            } catch (Exception e) {
                logger.error("[SubscriberFactory] Failed to instantiate subscriber={} class={}", subscriberId,
                        className, e);
            }
        }
    }
}
