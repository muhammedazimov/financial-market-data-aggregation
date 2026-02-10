package com.simulator.pf1simulator;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Pf1SimulatorApplication implements CommandLineRunner {
    public static void main(String[] args) {
        SpringApplication.run(Pf1SimulatorApplication.class, args);
    }

    @Override
    public void run(String... args) {
        new Thread(() -> {
            try {
                new TcpSimulatorServer().start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
