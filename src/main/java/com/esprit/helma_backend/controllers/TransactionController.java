package com.esprit.helma_backend.controllers;

import com.esprit.helma_backend.dto.TransactionDto;
import com.esprit.helma_backend.services.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionDto.Response create(@Valid @RequestBody TransactionDto.Create req) {
        return service.create(req);
    }

    @GetMapping("/{id}")
    public TransactionDto.Response getById(@PathVariable Long id) {
        return service.getById(id);
    }

    // GET /api/transactions OR /api/transactions?userId=1
    @GetMapping
    public List<TransactionDto.Response> getAll(@RequestParam(required = false) Long userId) {
        if (userId != null) return service.getByUser(userId);
        return service.getAll();
    }

    @PutMapping("/{id}")
    public TransactionDto.Response update(@PathVariable Long id, @Valid @RequestBody TransactionDto.Update req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
