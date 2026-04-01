package tn.esprit.helma.schedulers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.esprit.helma.services.ITransactionService;

/**
 * Scheduler centralisant l'exécution des transactions programmées et permanentes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionScheduler {

    private final ITransactionService transactionService;

    /**
     * Vérifie chaque minute si des transactions programmées sont arrivées à échéance.
     */
    @Scheduled(cron = "0 * * * * ?")
    public void triggerScheduledTransactions() {
        log.debug("Démarrage de l'exécution des transactions programmées");
        transactionService.executeScheduledTransactions();
    }

    /**
     * Vérifie chaque heure les transactions permanentes dont la prochaine exécution est arrivée.
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void triggerPermanentTransactions() {
        log.debug("Démarrage de l'exécution des transactions permanentes");
        transactionService.executePermanentTransactions();
    }
}
