package com.helma.helmabackend.integration.mailjet;

import com.mailjet.client.MailjetClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailjetConfig {

    @Bean
    public MailjetClient mailjetClient(
            @Value("${mailjet.apiKey}") String apiKey,
            @Value("${mailjet.secretKey}") String secretKey
    ) {
        return new MailjetClient(apiKey, secretKey);
    }
}