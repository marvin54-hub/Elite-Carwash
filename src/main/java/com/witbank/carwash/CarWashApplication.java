package com.witbank.carwash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CarWashApplication {

    public static void main(String[] args) {
        // Fail fast with a clear message if Java version is wrong
        int version = Runtime.version().feature();
        if (version < 17) {
            System.err.println("\n╔══════════════════════════════════════════════════════════════╗");
            System.err.println("║  ERROR: Java " + version + " detected. This project requires Java 17+.   ║");
            System.err.println("║  Download Java 17: https://adoptium.net/temurin/releases/    ║");
            System.err.println("╚══════════════════════════════════════════════════════════════╝\n");
            System.exit(1);
        }
        SpringApplication.run(CarWashApplication.class, args);
    }
}
