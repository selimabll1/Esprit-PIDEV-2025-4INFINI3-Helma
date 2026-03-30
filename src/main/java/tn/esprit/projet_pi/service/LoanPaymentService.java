package tn.esprit.projet_pi.service;

import org.springframework.stereotype.Service;
import tn.esprit.projet_pi.entity.LoanPayment;
import tn.esprit.projet_pi.repository.LoanPaymentRepository;

import java.util.List;

@Service
public class LoanPaymentService {

    private final LoanPaymentRepository repository;

    public LoanPaymentService(LoanPaymentRepository repository) {
        this.repository = repository;
    }

    public LoanPayment save(LoanPayment payment) {
        return repository.save(payment);
    }

    public List<LoanPayment> getAll() {
        return repository.findAll();
    }

    public LoanPayment getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}

