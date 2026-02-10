package com.exchange.logging;

import org.apache.http.HttpHost;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.LogEvent;
import org.opensearch.client.*;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.common.xcontent.XContentType;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// Log4j2 için custom appender: logları OpenSearch'e yazar
@Plugin(name = "OpenSearchAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class OpenSearchAppender extends AbstractAppender {

    private final RestHighLevelClient client; // OpenSearch istemcisi
    private final String indexName; // Logların yazılacağı index adı
    private final String serviceName; // Servis adı (hangi uygulamadan geldiği)

    // Constructor
    protected OpenSearchAppender(String name,
            Filter filter,
            Layout<? extends Serializable> layout,
            boolean ignoreExceptions,
            RestHighLevelClient client,
            String indexName,
            String serviceName) {
        super(name, filter, layout, ignoreExceptions);
        this.client = client;
        this.indexName = indexName;
        this.serviceName = serviceName;
    }

    // Log4j2.xml dosyasından appender oluşturmak için plugin factory
    @PluginFactory
    public static OpenSearchAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginAttribute("indexName") String indexName,
            @PluginAttribute("serviceName") String serviceName,
            @PluginAttribute("host") String host) {

        String osHost = (host != null && !host.isEmpty()) ? host : "localhost";

        // OpenSearch istemcisi oluştur
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost(osHost, 9200, "http")));

        // Appender nesnesini döndür
        return new OpenSearchAppender(name, null,
                PatternLayout.createDefaultLayout(), true,
                client, indexName, serviceName);
    }

    // Log eventi geldiğinde çağrılır → OpenSearch'e yazılır
    @Override
    public void append(LogEvent event) {
        try {
            Map<String, Object> json = new HashMap<>();
            json.put("timestamp", new Date(event.getTimeMillis())); // Log zamanı
            json.put("level", event.getLevel().toString()); // Log seviyesi
            json.put("logger", event.getLoggerName()); // Logger ismi
            json.put("thread", event.getThreadName()); // Thread bilgisi
            json.put("message", event.getMessage().getFormattedMessage()); // Log mesajı

            // Ek bilgiler
            json.put("service", serviceName); // Hangi servis
            json.put("host", InetAddress.getLocalHost().getHostName()); // Host adı

            // OpenSearch'e index request oluştur
            IndexRequest request = new IndexRequest(indexName)
                    .source(json, XContentType.JSON);

            // Indexleme işlemini yap
            client.index(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
