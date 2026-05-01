package com.helma.helmabackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration CORS pour autoriser les requêtes depuis le frontend Angular
 * 
 * Cette configuration permet au frontend (http://localhost:4200) de communiquer
 * avec le backend Spring Boot sans problèmes de CORS.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        
        // Autoriser les origines du frontend Angular
        corsConfiguration.setAllowedOrigins(Arrays.asList(
            "http://localhost:4200",      // Frontend Angular en développement
            "http://localhost:4201",      // Port alternatif si nécessaire
            "http://127.0.0.1:4200"       // Variante avec 127.0.0.1
        ));
        
        // Autoriser tous les headers
        corsConfiguration.setAllowedHeaders(Arrays.asList(
            "Origin",
            "Content-Type",
            "Accept",
            "Authorization",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "X-Requested-With"
        ));
        
        // Exposer les headers nécessaires au frontend
        corsConfiguration.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials",
            "Authorization"
        ));
        
        // Autoriser toutes les méthodes HTTP
        corsConfiguration.setAllowedMethods(Arrays.asList(
            "GET",
            "POST",
            "PUT",
            "DELETE",
            "PATCH",
            "OPTIONS"
        ));
        
        // Autoriser les credentials (cookies, authorization headers)
        corsConfiguration.setAllowCredentials(true);
        
        // Durée de cache de la configuration CORS (en secondes)
        corsConfiguration.setMaxAge(3600L);
        
        // Appliquer la configuration à tous les endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        
        return new CorsFilter(source);
    }
}
