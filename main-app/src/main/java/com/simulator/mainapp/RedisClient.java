package com.simulator.mainapp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;

public class RedisClient {

    private static final Logger logger = LogManager.getLogger(RedisClient.class);

    private Jedis jedis; // Redis bağlantısı için Jedis nesnesi

    // Yapıcı metod -> Redis'e bağlanmayı dener
    public RedisClient(String host, int port) {
        connect(host, port);
    }

    // Redis'e bağlantı kuran metod
    private void connect(String host, int port) {
        try {
            jedis = new Jedis(host, port); // Redis istemcisi oluştur
            jedis.connect();               // Bağlantıyı aç
            logger.info("[RedisClient] Connected to Redis {}:{}", host, port);
        } catch (Exception e) {
            logger.error("[RedisClient] Failed to connect to Redis {}:{}", host, port, e);
        }
    }

    // Redis'e değer yazma (SET komutu)
    public synchronized void set(String key, String value) {
        try {
            // Eğer bağlantı yoksa ya da kopmuşsa yeniden bağlanmayı dene
            if (jedis == null || !jedis.isConnected()) {
                logger.warn("[RedisClient] Redis not connected, reconnecting...");
                connect("127.0.0.1", 6379); // Default olarak localhost:6379’a bağlan
            }
            // Redis’e değer yaz
            jedis.set(key, value);
            logger.debug("[RedisClient] SET key={} value={}", key, value);
        } catch (Exception e) {
            logger.error("[RedisClient] Error setting key={} value={}", key, value, e);
        }
    }
}
