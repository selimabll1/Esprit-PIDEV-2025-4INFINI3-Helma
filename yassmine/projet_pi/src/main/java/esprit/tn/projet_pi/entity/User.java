package esprit.tn.projet_pi.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "app_user")

public class User {

    @Id
    private Long id;

    private String name;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<SavingsGoal> goals;

    // getters setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<SavingsGoal> getGoals() { return goals; }
    public void setGoals(List<SavingsGoal> goals) { this.goals = goals; }
}
