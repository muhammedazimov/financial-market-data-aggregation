package com.consumer.dbconsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.common.xcontent.XContentType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaOpenSearchConsumer {

    private static final Logger logger = LogManager.getLogger(KafkaOpenSearchConsumer.class);

    private final RestHighLevelClient client; // OpenSearch istemcisi

    // Constructor injection ile OpenSearch client alınır
    public KafkaOpenSearchConsumer(RestHighLevelClient client) {
        this.client = client;
    }

    // Kafka'dan "rates-topic" topic'ini dinler
    @KafkaListener(topics = "rates-topic", groupId = "opensearch-writer-group")
    public void consume(String message) {
        logger.info("[Kafka→OS] Received message: {}", message);

        try {
            // OpenSearch'e yazılacak index isteği oluştur
            IndexRequest request = new IndexRequest("rates")
                    .source("{\"message\":\"" + message + "\"}", XContentType.JSON);

            // Indexleme işlemini yap
            IndexResponse response = client.index(request, RequestOptions.DEFAULT);

            // Başarılı olursa logla
            if (response.getId() != null) {
                logger.info("[OpenSearch] Indexed successfully: index={}, id={}",
                        response.getIndex(), response.getId());
            } else {
                // ID dönmezse uyarı logu
                logger.warn("[OpenSearch] Index response returned null id for message: {}", message);
            }
        } catch (Exception e) {
            // Hata olursa logla
            logger.error("[OpenSearch] Error indexing message: {}", message, e);
        }
    }
}
