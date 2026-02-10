package com.consumer.dbconsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
public class KafkaDbConsumer {

    private final RateRepository rateRepository; // DB'ye yazma için repository
    private static final Logger logger = LogManager.getLogger(KafkaDbConsumer.class);

    // Constructor injection ile repository alınır
    public KafkaDbConsumer(RateRepository rateRepository) {
        this.rateRepository = rateRepository;
    }

    // Kafka'dan "rates-topic" topic'ini dinler, her mesaj consume metoduna düşer
    @KafkaListener(topics = "rates-topic", groupId = "db-writer-group")
    public void consume(String message) {
        logger.info("[DB-Consumer] Received from Kafka: {}", message);

        try {
            // Mesajı parçala
            String[] parts = message.split("\\|");
            if (parts.length < 4) {
                logger.warn("[DB-Consumer] Invalid message format: {}", message);
                return;
            }

            // Rate bilgilerini al
            String rateName = parts[0];
            Double bid = Double.parseDouble(parts[1]);
            Double ask = Double.parseDouble(parts[2]);
            Timestamp rateUpdateTime = parseTimestamp(parts[3]);

            // DB entity oluştur
            RateEntity entity = new RateEntity();
            entity.setRateName(rateName);
            entity.setBid(bid);
            entity.setAsk(ask);
            entity.setRateUpdatetime(rateUpdateTime); // rate’in timestamp’i
            entity.setDbUpdatetime(new Timestamp(System.currentTimeMillis())); // DB'ye yazıldığı an

            // DB'ye kaydet
            rateRepository.save(entity);
            logger.info("[DB-Consumer] Saved to DB: {}", entity);

        } catch (Exception e) {
            logger.error("[DB-Consumer] Error parsing/saving message: {}", message, e);
        }
    }

    /**
     * Timestamp farklı formatlarda gelebileceği için burada esnek parse yapılır.
     * Deneme sırası: Instant, nanosecond ISO, millisec ISO, space format
     */
    private Timestamp parseTimestamp(String ts) {
        // 1) ISO string (örn: 2025-08-21T18:38:18Z)
        try {
            return Timestamp.from(Instant.parse(ts));
        } catch (Exception ignored) {}

        // 2) Nanosecond destekli (örn: 2025-08-21T18:38:18.049881900)
        try {
            LocalDateTime ldt = LocalDateTime.parse(ts, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS"));
            return Timestamp.valueOf(ldt);
        } catch (Exception ignored) {}

        // 3) Alternatif formatlar (millisec veya space)
        List<String> patterns = Arrays.asList(
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss.SSS"
        );

        for (String p : patterns) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(ts, DateTimeFormatter.ofPattern(p));
                return Timestamp.valueOf(ldt);
            } catch (Exception ignored) {}
        }

        // Hiçbiri uymazsa hata fırlat
        throw new IllegalArgumentException("Unsupported timestamp format: " + ts);
    }
}
