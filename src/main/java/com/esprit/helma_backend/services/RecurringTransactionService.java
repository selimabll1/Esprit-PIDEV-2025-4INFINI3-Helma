package com.esprit.helma_backend.services;

import com.esprit.helma_backend.dto.RecurringTransactionDto;
import com.esprit.helma_backend.dto.TransactionDto;
import com.esprit.helma_backend.entities.RecurringTransaction;
import com.esprit.helma_backend.entities.RecurringTransaction.Frequency;
import com.esprit.helma_backend.entities.User;
import com.esprit.helma_backend.repositories.RecurringTransactionRepository;
import com.esprit.helma_backend.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class RecurringTransactionService {

    private static final Logger log = LoggerFactory.getLogger(RecurringTransactionService.class);

    private final RecurringTransactionRepository repo;
    private final UserRepository userRepo;
    private final TransactionService transactionService;

    public RecurringTransactionService(RecurringTransactionRepository repo,
                                       UserRepository userRepo,
                                       TransactionService transactionService) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.transactionService = transactionService;
    }

    private static RecurringTransactionDto.Response toResponse(RecurringTransaction rt) {
        return new RecurringTransactionDto.Response(
                rt.getId(),
                rt.getUser().getId(),
                rt.getAmount(),
                rt.getCategory(),
                rt.getType(),
                rt.getFrequency(),
                rt.getNextDate(),
                rt.isActive(),
                rt.getDescription()
        );
    }

    @Transactional
    public RecurringTransactionDto.Response create(RecurringTransactionDto.Create req) {
        User user = userRepo.findById(req.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + req.userId()));

        RecurringTransaction rt = RecurringTransaction.builder()
                .user(user)
                .amount(req.amount())
                .category(req.category())
                .type(req.type())
                .frequency(req.frequency())
                .nextDate(LocalDate.now())
                .active(true)
                .description(req.description())
                .build();

        return toResponse(repo.save(rt));
    }

    @Transactional(readOnly = true)
    public List<RecurringTransactionDto.Response> getByUser(Long userId) {
        if (!userRepo.existsById(userId)) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        return repo.findByUserId(userId).stream().map(RecurringTransactionService::toResponse).toList();
    }

    @Transactional
    public RecurringTransactionDto.Response toggleActive(Long id) {
        RecurringTransaction rt = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("RecurringTransaction not found: " + id));
        rt.setActive(!rt.isActive());
        return toResponse(repo.save(rt));
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("RecurringTransaction not found: " + id);
        }
        repo.deleteById(id);
    }

    public void processAllDue() {
        LocalDate today = LocalDate.now();
        List<RecurringTransaction> due = repo.findAllByActiveTrueAndNextDateLessThanEqual(today);

        log.info("Processing {} due recurring transaction(s) for {}", due.size(), today);

        for (RecurringTransaction rt : due) {
            try {
                transactionService.create(new TransactionDto.Create(
                        rt.getUser().getId(),
                        rt.getAmount(),
                        rt.getCategory(),
                        rt.getType(),
                        null
                ));
                rt.setNextDate(advance(rt.getNextDate(), rt.getFrequency()));
                repo.save(rt);
                log.debug("Processed recurring tx id={}, next={}", rt.getId(), rt.getNextDate());
            } catch (Exception e) {
                log.error("Failed to process recurring tx id={}: {}", rt.getId(), e.getMessage(), e);
            }
        }
    }

    private LocalDate advance(LocalDate date, Frequency frequency) {
        return switch (frequency) {
            case DAILY -> date.plusDays(1);
            case WEEKLY -> date.plusDays(7);
            case MONTHLY -> date.plusDays(30);
        };
    }
}
