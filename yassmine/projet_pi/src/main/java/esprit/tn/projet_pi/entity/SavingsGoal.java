package esprit.tn.projet_pi.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "savings_goal")
public class SavingsGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private double targetAmount;

    private double currentAmount;

    private LocalDate creationDate;

    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    private GoalStatus status;

    // 1 goal -> plusieurs dépôts
    @OneToMany(mappedBy = "savingsGoal", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore

    private List<SavingsDeposit> deposits;

    // 1 goal -> 1 voucher
    @OneToOne(mappedBy = "savingsGoal", cascade = CascadeType.ALL)
    @JsonIgnore

    private Voucher voucher;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore

    private User user;


    // ===== Getters & Setters =====
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }

    public double getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(double currentAmount) { this.currentAmount = currentAmount; }

    public LocalDate getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDate creationDate) { this.creationDate = creationDate; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public GoalStatus getStatus() { return status; }
    public void setStatus(GoalStatus status) { this.status = status; }

    public List<SavingsDeposit> getDeposits() { return deposits; }
    public void setDeposits(List<SavingsDeposit> deposits) { this.deposits = deposits; }

    public Voucher getVoucher() { return voucher; }
    public void setVoucher(Voucher voucher) { this.voucher = voucher; }
}
