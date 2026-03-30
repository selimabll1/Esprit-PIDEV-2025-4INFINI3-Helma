package tn.esprit.projet_pi.service;

import org.springframework.stereotype.Service;
import tn.esprit.projet_pi.entity.LoanTransaction;
import tn.esprit.projet_pi.repository.LoanTransactionRepository;

import java.util.List;

@Service
public class LoanTransactionService {

    private final LoanTransactionRepository repository;

    public LoanTransactionService(LoanTransactionRepository repository) {
        this.repository = repository;
    }

    public LoanTransaction save(LoanTransaction transaction) {
        return repository.save(transaction);
    }

    public List<LoanTransaction> getAll() {
        return repository.findAll();
    }

    public LoanTransaction getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
