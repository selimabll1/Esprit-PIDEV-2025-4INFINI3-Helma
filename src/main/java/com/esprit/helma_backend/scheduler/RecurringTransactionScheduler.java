package com.esprit.helma_backend.scheduler;

import com.esprit.helma_backend.services.RecurringTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecurringTransactionScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecurringTransactionScheduler.class);

    private final RecurringTransactionService recurringTransactionService;

    public RecurringTransactionScheduler(RecurringTransactionService recurringTransactionService) {
        this.recurringTransactionService = recurringTransactionService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void runDailyProcessing() {
        log.info("Recurring transaction scheduler triggered");
        recurringTransactionService.processAllDue();
    }
}
