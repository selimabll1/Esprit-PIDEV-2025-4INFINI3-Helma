package esprit.tn.projet_pi.controller;

import esprit.tn.projet_pi.entity.SavingsDeposit;
import esprit.tn.projet_pi.service.DepositPdfService;
import esprit.tn.projet_pi.service.SavingsDepositService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/deposits")
public class SavingsDepositController {

    private final SavingsDepositService service;
    private final DepositPdfService depositPdfService;

    public SavingsDepositController(SavingsDepositService service , DepositPdfService depositPdfService) {
        this.service = service;
        this.depositPdfService = depositPdfService;
    }

    // deposits of user
    @GetMapping("/user/{userId}")
    public List<SavingsDeposit> getDepositsByUser(@PathVariable Long userId){
        return service.getDepositsByUser(userId);
    }

    // deposits of goal
    @GetMapping("/goal/{goalId}")
    public List<SavingsDeposit> getDepositsByGoal(@PathVariable Long goalId){
        return service.getDepositsByGoal(goalId);
    }

    // add deposit
    @PostMapping("/{goalId}")
    public SavingsDeposit deposit(
            @PathVariable Long goalId,
            @RequestParam double amount){

        return service.addDeposit(goalId, amount);
    }

    // ⭐ EDIT DEPOSIT
    @PutMapping("/{depositId}")
    public SavingsDeposit updateDeposit(
            @PathVariable Long depositId,
            @RequestParam double amount){

        return service.updateDeposit(depositId, amount);
    }

    // ⭐ DELETE DEPOSIT
    @DeleteMapping("/{depositId}")
    public void deleteDeposit(@PathVariable Long depositId){
        service.deleteDeposit(depositId);
    }





    // recherche avancée
    @GetMapping("/search/{userId}")
    public List<SavingsDeposit> searchDeposits(
            @PathVariable Long userId,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {

        List<SavingsDeposit> deposits = service.getDepositsByUser(userId);

        return deposits.stream()

                // montant min
                .filter(d -> minAmount == null || d.getAmount() >= minAmount)

                // montant max
                .filter(d -> maxAmount == null || d.getAmount() <= maxAmount)

                // date debut
                .filter(d -> startDate == null ||
                        !d.getDateDeposit().isBefore(LocalDate.parse(startDate)))

                // date fin
                .filter(d -> endDate == null ||
                        !d.getDateDeposit().isAfter(LocalDate.parse(endDate)))

                .sorted((d1,d2)->d2.getDateDeposit().compareTo(d1.getDateDeposit()))

                .collect(Collectors.toList());
    }



    //pdf
    @GetMapping(value="/statement/{userId}", produces = "application/pdf")
    public byte[] getStatement(@PathVariable Long userId){
        List<SavingsDeposit> deposits = service.getDepositsByUser(userId);
        return depositPdfService.generateDepositsPdf(deposits);
    }


}
