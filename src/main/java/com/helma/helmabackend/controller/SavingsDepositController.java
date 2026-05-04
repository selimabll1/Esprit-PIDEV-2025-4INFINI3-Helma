package com.helma.helmabackend.controller;


import com.helma.helmabackend.entity.SavingsDeposit;
import com.helma.helmabackend.service.DepositPdfService;
import com.helma.helmabackend.service.SavingsDepositService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/deposits")
@CrossOrigin(origins = "*")
public class SavingsDepositController {

    private final SavingsDepositService service;
    private final DepositPdfService depositPdfService;

    public SavingsDepositController(
            SavingsDepositService service,
            DepositPdfService depositPdfService
    ) {
        this.service = service;
        this.depositPdfService = depositPdfService;
    }

    /* =====================================================
       GET ALL DEPOSITS (ADMIN PAGE)
       /api/deposits
    ===================================================== */
    @GetMapping
    public List<SavingsDeposit> getAllDeposits() {
        return service.getAllDeposits();
    }

    /* =====================================================
       GET ONE DEPOSIT
       /api/deposits/5
    ===================================================== */
    @GetMapping("/{depositId}")
    public SavingsDeposit getOneDeposit(
            @PathVariable Long depositId
    ) {
        return service.getDepositById(depositId);
    }

    /* =====================================================
       GET DEPOSITS BY USER
       /api/deposits/user/1
    ===================================================== */
    @GetMapping("/user/{userId}")
    public List<SavingsDeposit> getDepositsByUser(
            @PathVariable Long userId
    ) {
        return service.getDepositsByUser(userId);
    }

    /* =====================================================
       GET DEPOSITS BY GOAL
       /api/deposits/goal/3
    ===================================================== */
    @GetMapping("/goal/{goalId}")
    public List<SavingsDeposit> getDepositsByGoal(
            @PathVariable Long goalId
    ) {
        return service.getDepositsByGoal(goalId);
    }

    /* =====================================================
       CREATE DEPOSIT
       POST /api/deposits/{goalId}?amount=150
    ===================================================== */
    @PostMapping("/{goalId}")
    public SavingsDeposit deposit(
            @PathVariable Long goalId,
            @RequestParam double amount
    ) {
        return service.addDeposit(goalId, amount);
    }

    /* =====================================================
       UPDATE DEPOSIT
       PUT /api/deposits/5?amount=200
    ===================================================== */
    @PutMapping("/{depositId}")
    public SavingsDeposit updateDeposit(
            @PathVariable Long depositId,
            @RequestParam double amount
    ) {
        return service.updateDeposit(depositId, amount);
    }

    /* =====================================================
       DELETE DEPOSIT
    ===================================================== */
    @DeleteMapping("/{depositId}")
    public void deleteDeposit(
            @PathVariable Long depositId
    ) {
        service.deleteDeposit(depositId);
    }

    /* =====================================================
       ADVANCED SEARCH
    ===================================================== */
    @GetMapping("/search/{userId}")
    public List<SavingsDeposit> searchDeposits(

            @PathVariable Long userId,

            @RequestParam(required = false)
            Double minAmount,

            @RequestParam(required = false)
            Double maxAmount,

            @RequestParam(required = false)
            String startDate,

            @RequestParam(required = false)
            String endDate
    ) {

        List<SavingsDeposit> deposits =
                service.getDepositsByUser(userId);

        return deposits.stream()

                .filter(d ->
                        minAmount == null ||
                                d.getAmount() >= minAmount
                )

                .filter(d ->
                        maxAmount == null ||
                                d.getAmount() <= maxAmount
                )

                .filter(d ->
                        startDate == null ||
                                !d.getDateDeposit()
                                        .isBefore(
                                                LocalDate.parse(startDate)
                                        )
                )

                .filter(d ->
                        endDate == null ||
                                !d.getDateDeposit()
                                        .isAfter(
                                                LocalDate.parse(endDate)
                                        )
                )

                .sorted((a,b) ->
                        b.getDateDeposit()
                                .compareTo(a.getDateDeposit())
                )

                .collect(Collectors.toList());
    }

    /* =====================================================
       PDF STATEMENT
    ===================================================== */
    @GetMapping(
            value = "/statement/{userId}",
            produces = "application/pdf"
    )
    public byte[] getStatement(
            @PathVariable Long userId
    ) {

        List<SavingsDeposit> deposits =
                service.getDepositsByUser(userId);

        return depositPdfService
                .generateDepositsPdf(deposits);
    }

}