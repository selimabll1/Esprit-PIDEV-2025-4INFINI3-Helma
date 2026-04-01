package com.helma.helmabackend.service.crowdfunding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helma.helmabackend.dto.crowdfunding.ai.AiCampaignInsightRequest;
import com.helma.helmabackend.dto.crowdfunding.ai.AiCampaignInsightResponse;
import com.helma.helmabackend.entity.crowdfunding.enums.AppTag;
import com.helma.helmabackend.dto.crowdfunding.ai.AiCampaignInsightBatchRequest;
import com.helma.helmabackend.dto.crowdfunding.ai.AiCampaignInsightBatchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiModelClientService {

    private static final Logger log = LoggerFactory.getLogger(AiModelClientService.class);

    private final String aiBaseUrl;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiModelClientService(@Value("${ai.model.base-url}") String aiBaseUrl) {
        this.aiBaseUrl = aiBaseUrl;
    }

    public Map<String, Object> getHealth() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                aiBaseUrl + "/health",
                Map.class
        );
        return response.getBody();
    }

    public AiCampaignInsightResponse getCampaignInsight(AiCampaignInsightRequest request) {
        Map<String, Object> payload = toPayload(request);

        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            log.info("Posting FastAPI /campaign-insight payload={}", jsonPayload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<AiCampaignInsightResponse> response = restTemplate.exchange(
                    aiBaseUrl + "/campaign-insight",
                    HttpMethod.POST,
                    entity,
                    AiCampaignInsightResponse.class
            );

            return response.getBody();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize AI payload to JSON", e);
        }
    }

    public AiCampaignInsightResponse getCampaignInsightSafe(AiCampaignInsightRequest request) {
        try {
            return getCampaignInsight(request);
        } catch (Exception ex) {
            log.error("FastAPI campaign insight call failed for campaignId={}, subSector={}, sector={}, type={}. Request={}",
                    request.campaignId,
                    request.subSector,
                    request.sector,
                    request.type,
                    request,
                    ex);
            return null;
        }
    }

    private Map<String, Object> toPayload(AiCampaignInsightRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("campaignId", request.campaignId);
        payload.put("businessName", request.businessName);
        payload.put("type", request.type != null ? request.type.name() : null);
        payload.put("sector", request.sector != null ? request.sector.name() : null);
        payload.put("subSector", request.subSector != null ? request.subSector.name() : null);
        payload.put("tags", request.tags != null
                ? request.tags.stream().map(AppTag::name).toList()
                : List.of());
        payload.put("summary", request.summary);
        payload.put("fundingGoal", request.fundingGoal);
        payload.put("investorsPledgedAmount", request.investorsPledgedAmount);
        payload.put("currency", request.currency);
        payload.put("equityOfferedPercent", request.equityOfferedPercent);
        payload.put("preMoneyValuation", request.preMoneyValuation);
        payload.put("minInvestment", request.minInvestment);
        return payload;
    }
    public AiCampaignInsightBatchResponse getCampaignInsightsBatch(List<AiCampaignInsightRequest> requests) {
        AiCampaignInsightBatchRequest batchRequest = new AiCampaignInsightBatchRequest();
        batchRequest.campaigns = requests;

        try {
            String jsonPayload = objectMapper.writeValueAsString(batchRequest);
            log.info("Posting FastAPI /campaign-insights/batch payload={}", jsonPayload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<AiCampaignInsightBatchResponse> response = restTemplate.exchange(
                    aiBaseUrl + "/campaign-insights/batch",
                    HttpMethod.POST,
                    entity,
                    AiCampaignInsightBatchResponse.class
            );

            return response.getBody();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize AI batch payload to JSON", e);
        }
    }
}