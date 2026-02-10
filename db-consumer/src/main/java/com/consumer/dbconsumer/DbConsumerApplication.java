package com.consumer.dbconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class DbConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbConsumerApplication.class, args);
    }

}
