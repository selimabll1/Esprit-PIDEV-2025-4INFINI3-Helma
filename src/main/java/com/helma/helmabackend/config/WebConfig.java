package com.helma.helmabackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration Web MVC alternative pour CORS
 * 
 * Cette configuration est une alternative à CorsConfig.java
 * Vous pouvez utiliser l'une ou l'autre, mais pas les deux en même temps.
 * 
 * Pour utiliser cette configuration, commentez ou supprimez CorsConfig.java
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // Autoriser le frontend Angular
                .allowedOrigins(
                    "http://localhost:4200",
                    "http://localhost:4201",
                    "http://127.0.0.1:4200"
                )
                // Autoriser toutes les méthodes HTTP
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                // Autoriser tous les headers
                .allowedHeaders("*")
                // Exposer les headers nécessaires
                .exposedHeaders("Authorization", "Access-Control-Allow-Origin")
                // Autoriser les credentials
                .allowCredentials(true)
                // Durée de cache
                .maxAge(3600);
    }
}
