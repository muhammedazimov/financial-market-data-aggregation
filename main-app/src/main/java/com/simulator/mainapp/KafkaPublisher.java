package com.simulator.mainapp;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;

public class KafkaPublisher {

    private static final Logger logger = LogManager.getLogger(KafkaPublisher.class);

    private final KafkaProducer<String, String> producer; // Kafka producer nesnesi
    private final String topic; // Mesajların gönderileceği topic

    // Yapıcı metod: Kafka producer'ı başlatır
    public KafkaPublisher(String bootstrapServers, String topic) {
        this.topic = topic;

        Properties props = new Properties();
        // Kafka broker adresi (örn: localhost:9092)
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Mesajın key kısmı string olacak
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        // Mesajın value kısmı string olacak
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Producer nesnesini oluştur
        this.producer = new KafkaProducer<>(props);
        logger.info("[KafkaPublisher] Initialized for topic={} bootstrapServers={}", topic, bootstrapServers);
    }

    // Kafka'ya mesaj gönderme metodu
    public void send(String key, String message) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, message);
        try {
            // Asenkron şekilde mesaj gönderiliyor
            producer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    // Mesaj başarıyla gönderildiyse log bas
                    logger.info("[KafkaPublisher] Sent message to topic={} partition={} offset={} key={} value={}",
                            metadata.topic(), metadata.partition(), metadata.offset(), key, message);
                } else {
                    // Hata durumunu logla
                    logger.error("[KafkaPublisher] Failed to send message key={} value={}", key, message, exception);
                }
            });
        } catch (Exception e) {
            // Beklenmeyen hata olursa yakala
            logger.error("[KafkaPublisher] Unexpected error while sending message key={} value={}", key, message, e);
        }
    }

    // Producer'ı kapatma metodu
    public void close() {
        producer.close();
        logger.info("[KafkaPublisher] Producer closed for topic={}", topic);
    }
}
