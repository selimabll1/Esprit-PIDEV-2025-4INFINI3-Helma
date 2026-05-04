package com.helma.helmabackend.service.crowdfunding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helma.helmabackend.dto.crowdfunding.rne.RneShortDetailsResponse;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class RneLookupService {

    private static final String BASE_URL =
            "https://www.registre-entreprises.tn/api/rne-api/front-office/entites/short-details/";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public RneShortDetailsResponse fetchShortDetails(String id) {
        String cleanedId = normalizeId(id);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + URLEncoder.encode(cleanedId, StandardCharsets.UTF_8)))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Unable to fetch RNE details right now.");
            }

            if (response.body() == null || response.body().isBlank()) {
                throw new IllegalStateException("RNE returned an empty response.");
            }

            return objectMapper.readValue(response.body(), RneShortDetailsResponse.class);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to fetch RNE details right now.", ex);
        }
    }

    private String normalizeId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("RNE identifier is required.");
        }

        return id.trim().toUpperCase();
    }
}