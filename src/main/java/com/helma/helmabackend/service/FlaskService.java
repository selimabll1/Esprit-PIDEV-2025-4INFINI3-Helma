package com.helma.helmabackend.service;

import com.helma.helmabackend.dto.PredictionRequest;
import com.helma.helmabackend.dto.PredictionResult;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FlaskService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String FLASK_URL = "http://localhost:5000/predict";

    public PredictionResult predict(PredictionRequest req) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PredictionRequest> entity = new HttpEntity<>(req, headers);

        ResponseEntity<PredictionResult> response = restTemplate.postForEntity(
                FLASK_URL, entity, PredictionResult.class
        );
        return response.getBody();
    }
}
