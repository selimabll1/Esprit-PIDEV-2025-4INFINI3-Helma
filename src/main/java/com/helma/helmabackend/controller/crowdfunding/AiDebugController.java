package com.helma.helmabackend.controller.crowdfunding;

import com.helma.helmabackend.dto.crowdfunding.ai.AiCampaignInsightRequest;
import com.helma.helmabackend.dto.crowdfunding.ai.AiCampaignInsightResponse;
import com.helma.helmabackend.service.crowdfunding.AiModelClientService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/crowdfunding/admin/ai")
public class AiDebugController {

    private final AiModelClientService aiModelClientService;

    public AiDebugController(AiModelClientService aiModelClientService) {
        this.aiModelClientService = aiModelClientService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return aiModelClientService.getHealth();
    }

    @PostMapping("/campaign-insight-test")
    public AiCampaignInsightResponse campaignInsightTest(@RequestBody AiCampaignInsightRequest request) {
        return aiModelClientService.getCampaignInsight(request);
    }
}