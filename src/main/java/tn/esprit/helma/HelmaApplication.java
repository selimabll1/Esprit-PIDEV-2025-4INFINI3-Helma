package tn.esprit.helma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HelmaApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelmaApplication.class, args);
    }

}
