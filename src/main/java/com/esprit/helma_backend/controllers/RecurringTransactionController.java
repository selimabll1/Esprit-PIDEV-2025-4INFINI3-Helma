package com.esprit.helma_backend.controllers;

import com.esprit.helma_backend.dto.RecurringTransactionDto;
import com.esprit.helma_backend.services.RecurringTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-transactions")
@Tag(name = "Recurring Transactions", description = "Manage scheduled recurring transactions")
public class RecurringTransactionController {

    private final RecurringTransactionService service;

    public RecurringTransactionController(RecurringTransactionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a recurring transaction")
    public RecurringTransactionDto.Response create(@Valid @RequestBody RecurringTransactionDto.Create req) {
        return service.create(req);
    }

    @GetMapping
    @Operation(summary = "List recurring transactions for a user")
    public List<RecurringTransactionDto.Response> getByUser(@RequestParam Long userId) {
        return service.getByUser(userId);
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Enable or disable a recurring transaction")
    public RecurringTransactionDto.Response toggle(@PathVariable Long id) {
        return service.toggleActive(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a recurring transaction")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
