package tn.esprit.projet_pi.service;

import org.springframework.stereotype.Service;
import tn.esprit.projet_pi.entity.RepaymentSchedule;
import tn.esprit.projet_pi.repository.RepaymentScheduleRepository;

import java.util.List;

@Service
public class RepaymentScheduleService {

    private final RepaymentScheduleRepository repository;

    public RepaymentScheduleService(RepaymentScheduleRepository repository) {
        this.repository = repository;
    }

    public RepaymentSchedule save(RepaymentSchedule schedule) {
        return repository.save(schedule);
    }

    public List<RepaymentSchedule> getAll() {
        return repository.findAll();
    }

    public RepaymentSchedule getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
