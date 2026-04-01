package com.esprit.helma_backend.controllers;

import com.esprit.helma_backend.dto.CoachDto;
import com.esprit.helma_backend.services.CoachService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coach")
@CrossOrigin(origins = "*")
public class CoachController {

    private final CoachService coachService;

    public CoachController(CoachService coachService) {
        this.coachService = coachService;
    }

    @PostMapping("/message")
    public ResponseEntity<CoachDto.Response> sendMessage(@RequestBody CoachDto.SendRequest req) {
        return ResponseEntity.ok(
                coachService.sendMessage(req.userId(), req.message())
        );
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<CoachDto.MessageResponse>> getHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(coachService.getHistory(userId));
    }
}