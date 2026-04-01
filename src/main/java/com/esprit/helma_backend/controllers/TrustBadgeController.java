package com.esprit.helma_backend.controllers;

import com.esprit.helma_backend.dto.TrustBadgeDto;
import com.esprit.helma_backend.services.TrustBadgeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/badge")
@CrossOrigin(origins = "*")
public class TrustBadgeController {

    private final TrustBadgeService trustBadgeService;

    public TrustBadgeController(TrustBadgeService trustBadgeService) {
        this.trustBadgeService = trustBadgeService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<TrustBadgeDto.Response> getBadge(@PathVariable Long userId) {
        return ResponseEntity.ok(trustBadgeService.getByUserId(userId));
    }
}