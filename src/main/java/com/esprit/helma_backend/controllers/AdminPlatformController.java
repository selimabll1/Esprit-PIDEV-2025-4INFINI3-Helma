package com.esprit.helma_backend.controllers;

import com.esprit.helma_backend.dto.AdminPlatformDto;
import com.esprit.helma_backend.entities.User;
import com.esprit.helma_backend.services.AdminPlatformService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/platform")
@Tag(name = "Admin – Platform", description = "Platform-level admin management: users, stats")
public class AdminPlatformController {

    private final AdminPlatformService service;

    public AdminPlatformController(AdminPlatformService service) {
        this.service = service;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get platform-wide statistics")
    public ResponseEntity<AdminPlatformDto.PlatformStats> getStats() {
        return ResponseEntity.ok(service.getStats());
    }

    @GetMapping("/users")
    @Operation(summary = "List all users across all roles")
    public ResponseEntity<List<AdminPlatformDto.UserRow>> getAllUsers() {
        return ResponseEntity.ok(service.getAllUsers());
    }

    @PatchMapping("/users/{id}/role")
    @Operation(summary = "Change a user's role")
    public ResponseEntity<AdminPlatformDto.UserRow> changeRole(
            @PathVariable Long id,
            @Valid @RequestBody AdminPlatformDto.RoleChangeRequest req) {
        return ResponseEntity.ok(service.changeRole(id, req.role()));
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a user account")
    public void deleteUser(@PathVariable Long id) {
        service.deleteUser(id);
    }
}
