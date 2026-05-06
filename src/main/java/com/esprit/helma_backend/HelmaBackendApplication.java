package com.esprit.helma_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HelmaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelmaBackendApplication.class, args);
    }

}
