package com.esprit.helma_backend.controllers;

import com.esprit.helma_backend.dto.RiskCaseDto;
import com.esprit.helma_backend.services.RiskCaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/risk-cases")
public class RiskCaseController {

    private final RiskCaseService service;

    public RiskCaseController(RiskCaseService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RiskCaseDto.Response create(@Valid @RequestBody RiskCaseDto.Create req) {
        return service.create(req);
    }

    @GetMapping("/{id}")
    public RiskCaseDto.Response getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public List<RiskCaseDto.Response> getAll(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long assignedAdminId
    ) {
        if (userId != null) return service.getByUser(userId);
        if (assignedAdminId != null) return service.getByAssignedAdmin(assignedAdminId);
        return service.getAll();
    }

    @PutMapping("/{id}")
    public RiskCaseDto.Response update(@PathVariable Long id, @Valid @RequestBody RiskCaseDto.Update req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
