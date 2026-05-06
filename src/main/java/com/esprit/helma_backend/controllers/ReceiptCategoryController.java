package com.esprit.helma_backend.controllers;

import com.esprit.helma_backend.dto.ReceiptScanDto;
import com.esprit.helma_backend.services.ReceiptScanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class ReceiptCategoryController {

    private final ReceiptScanService service;

    public ReceiptCategoryController(ReceiptScanService service) {
        this.service = service;
    }

    /**
     * POST /api/transactions/suggest-category
     * Body: { "base64Image": "data:image/jpeg;base64,..." }
     * Returns: ReceiptScanDto { category, type, amount, date, description }
     */
    @PostMapping("/suggest-category")
    public ResponseEntity<ReceiptScanDto> suggestCategory(
            @RequestBody Map<String, String> body) {

        String base64Image = body.get("base64Image");
        if (base64Image == null || base64Image.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(service.scan(base64Image));
    }
}
