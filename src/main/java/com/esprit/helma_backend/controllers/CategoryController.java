package com.esprit.helma_backend.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.esprit.helma_backend.services.CategoryService;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /** Returns the full master list grouped by section */
    @GetMapping
    public ResponseEntity<Map<String, List<String>>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getMasterList());
    }

    /** Returns this user's top used categories (sorted by frequency) */
    @GetMapping("/user/{userId}/frequent")
    public ResponseEntity<List<String>> getFrequent(@PathVariable Long userId) {
        return ResponseEntity.ok(categoryService.getFrequent(userId));
    }

    /** Called after a transaction is saved — increments usage count */
    @PostMapping("/user/{userId}/track")
    public ResponseEntity<Void> track(@PathVariable Long userId,
                                       @RequestBody Map<String, String> body) {
        String category = body.get("category");
        if (category != null && !category.isBlank()) {
            categoryService.track(userId, category.trim());
        }
        return ResponseEntity.ok().build();
    }
}