package esprit.tn.projet_pi.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "savings_deposit")
public class SavingsDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double amount;

    private LocalDate dateDeposit;

    // plusieurs dépôts appartiennent à 1 goal
    @ManyToOne
    @JoinColumn(name = "goal_id")
    @JsonIgnore

    private SavingsGoal savingsGoal;

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDate getDateDeposit() { return dateDeposit; }
    public void setDateDeposit(LocalDate dateDeposit) { this.dateDeposit = dateDeposit; }

    public SavingsGoal getSavingsGoal() { return savingsGoal; }
    public void setSavingsGoal(SavingsGoal savingsGoal) { this.savingsGoal = savingsGoal; }
}
