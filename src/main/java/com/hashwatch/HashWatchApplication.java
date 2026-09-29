package com.hashwatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * HashWatch Application Entry Point
 * Cryptographically Signed File Integrity Monitoring System
 */
@SpringBootApplication
@EnableScheduling
public class HashWatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(HashWatchApplication.class, args);
    }
}
