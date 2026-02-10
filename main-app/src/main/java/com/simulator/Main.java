package com.simulator;

import com.simulator.mainapp.Coordinator;
import com.simulator.mainapp.KafkaPublisher;
import com.simulator.mainapp.RedisClient;
import com.simulator.mainapp.SubscriberFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Main {

    private static final List<Process> childProcesses = new ArrayList<>();
    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        // Load Config
        String configPath = "config.properties";
        if (args.length > 0) {
            configPath = args[0];
        } else {
            // Fallback to classpath resource or default file
            // If file not found in CWD, we might need to load from classpath.
            // But for now assume it's in CWD or passed arg.
            // Dockerfile copies it to /app/config.properties so it will be found.
        }

        Properties props = new Properties();
        File configFile = new File(configPath);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            }
        } else {
            logger.warn("Config file not found at {}, using defaults/classpath", configPath);
            // Fallback: load from classpath
            try (var stream = Main.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (stream != null)
                    props.load(stream);
            }
        }

        // Check if we should start simulators (Local dev mode)
        String startSimEnv = System.getenv("START_SIMULATORS");
        boolean startSimulators = startSimEnv != null ? Boolean.parseBoolean(startSimEnv)
                : Boolean.parseBoolean(props.getProperty("start.simulators", "false"));

        if (startSimulators) {
            try {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    logger.info("Shutting down... killing child processes.");
                    for (Process p : childProcesses) {
                        if (p.isAlive()) {
                            p.destroy();
                        }
                    }
                }));

                startJar("pf1-simulator/target/pf1-simulator-0.0.1-SNAPSHOT-tcp.jar");
                startJar("pf2-simulator/target/pf2-simulator-0.0.1-SNAPSHOT.jar", "--server.port=8081");
                Thread.sleep(5000);
                startJar("db-consumer/target/db-consumer-0.0.1-SNAPSHOT.jar", "--server.port=8082");

            } catch (Exception e) {
                logger.error("Error starting child processes", e);
            }
        }

        // Redis & Kafka Configuration
        String redisHost = System.getenv("REDIS_HOST") != null ? System.getenv("REDIS_HOST")
                : props.getProperty("redis.host", "localhost");
        int redisPort = System.getenv("REDIS_PORT") != null ? Integer.parseInt(System.getenv("REDIS_PORT"))
                : Integer.parseInt(props.getProperty("redis.port", "6379"));
        String kafkaServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS") != null
                ? System.getenv("KAFKA_BOOTSTRAP_SERVERS")
                : props.getProperty("kafka.bootstrap.servers", "localhost:9092");

        // Init Components
        RedisClient redis = new RedisClient(redisHost, redisPort);
        KafkaPublisher publisher = new KafkaPublisher(kafkaServers, "rates-topic");
        Coordinator coordinator = new Coordinator(redis, publisher);

        // SubscriberFactory
        // We need to pass the Properties or params to SubscriberFactory?
        // SubscriberFactory currently takes path.
        // We should overload it or pass path.
        // Since we already resolved the path (or it's in CWD), we can pass configPath.
        // If configPath was implicit (classpath), SubscriberFactory might fail if it
        // demands a file path.
        // I should update SubscriberFactory to take Properties OR handle classpath.
        // But for Docker, config.properties is a file. So passing configPath is fine.

        SubscriberFactory factory = new SubscriberFactory(coordinator, configPath);
        factory.startAllSubscribers();

        logger.info("System started. Waiting for rate updates...");
        Thread.currentThread().join();
    }

    private static void startJar(String jarPath, String... args) throws Exception {
        File jarFile = new File(jarPath);
        if (!jarFile.exists()) {
            logger.error("Jar not found: {}", jarFile.getAbsolutePath());
            return;
        }

        ProcessBuilder pb = new ProcessBuilder();
        pb.command().add("java");
        pb.command().add("-jar");
        pb.command().add(jarFile.getAbsolutePath());
        for (String arg : args) {
            pb.command().add(arg);
        }

        pb.inheritIO();
        Process process = pb.start();
        childProcesses.add(process);
        logger.info("Started: {}", jarFile.getName());
    }
}
