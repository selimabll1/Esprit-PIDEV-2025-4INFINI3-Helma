package tn.esprit.projet_pi.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.projet_pi.entity.RepaymentSchedule;
import tn.esprit.projet_pi.service.RepaymentScheduleService;

import java.util.List;

@RestController
@RequestMapping("/api/repayment-schedules") // 🔥 cohérent avec /api
@CrossOrigin("*")
public class RepaymentScheduleController {

    private final RepaymentScheduleService service;

    public RepaymentScheduleController(RepaymentScheduleService service) {
        this.service = service;
    }

    // 🔥 CREATE
    @PostMapping
    public ResponseEntity<RepaymentSchedule> create(@Valid @RequestBody RepaymentSchedule schedule) {
        RepaymentSchedule saved = service.save(schedule);
        return ResponseEntity.status(201).body(saved);
    }

    // 🔥 READ ALL
    @GetMapping
    public ResponseEntity<List<RepaymentSchedule>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    // 🔥 READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<RepaymentSchedule> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    // 🔥 DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build(); // 204
    }
}