package com.esprit.helma_backend.controllers;

import com.esprit.helma_backend.services.GroqApiClient;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test/groq")
@CrossOrigin(origins = "*")
public class GroqTestController {

    private final GroqApiClient groqApiClient;
    private final ConfigurableEnvironment environment;

    public GroqTestController(GroqApiClient groqApiClient, ConfigurableEnvironment environment) {
        this.groqApiClient = groqApiClient;
        this.environment = environment;
    }

    @GetMapping("/debug-key")
    public ResponseEntity<String> debugKey() {
        return ResponseEntity.ok(groqApiClient.getKeyDebugInfo());
    }

    @GetMapping("/property-sources")
    public ResponseEntity<List<String>> propertySources() {
        List<String> result = new ArrayList<>();

        for (PropertySource<?> ps : environment.getPropertySources()) {
            Object value = ps.getProperty("groq.api.key");
            if (value != null) {
                String s = value.toString();
                String masked = mask(s);
                result.add(ps.getName() + " => " + masked + " (len=" + s.length() + ")");
            }
        }

        String resolved = environment.getProperty("groq.api.key");
        result.add("RESOLVED => " + mask(resolved) + " (len=" + (resolved == null ? 0 : resolved.length()) + ")");

        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<String> test(@RequestBody Map<String, String> body) {
        String prompt = body.getOrDefault("prompt", "Donne-moi un conseil financier simple.");

        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", "Tu es un coach financier clair et concis."),
                Map.of("role", "user", "content", prompt)
        );

        try {
            String reply = groqApiClient.chat(messages);
            return ResponseEntity.ok(reply);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Groq test failed: " + e.getMessage());
        }
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) return "NONE";
        String trimmed = value.trim();
        if (trimmed.length() <= 10) return trimmed;
        return trimmed.substring(0, 6) + "..." + trimmed.substring(trimmed.length() - 4);
    }
}