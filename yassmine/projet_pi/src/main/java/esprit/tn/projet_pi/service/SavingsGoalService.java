package esprit.tn.projet_pi.service;

import esprit.tn.projet_pi.entity.GoalStatus;
import esprit.tn.projet_pi.entity.SavingsGoal;
import esprit.tn.projet_pi.entity.User;
import esprit.tn.projet_pi.repository.SavingsGoalRepository;
import esprit.tn.projet_pi.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SavingsGoalService {

    private final SavingsGoalRepository goalRepository;
    private final UserRepository userRepository;

    public SavingsGoalService(SavingsGoalRepository goalRepository, UserRepository userRepository) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
    }

    // CREATE
    public SavingsGoal createGoal(SavingsGoal goal){

        User user = userRepository.findById(1L).orElseThrow();

        goal.setUser(user);
        goal.setCreationDate(LocalDate.now());
        goal.setCurrentAmount(0);
        goal.setStatus(GoalStatus.IN_PROGRESS);

        return goalRepository.save(goal);
    }

    // READ
    public List<SavingsGoal> getUserGoals(){
        return goalRepository.findByUserId(1L);
    }

    // DELETE
    public void deleteGoal(Long id){
        goalRepository.deleteById(id);
    }

    // UPDATE
    public SavingsGoal updateGoal(Long id, SavingsGoal newGoal){

        SavingsGoal goal = goalRepository.findById(id).orElseThrow();

        goal.setTitle(newGoal.getTitle());
        goal.setTargetAmount(newGoal.getTargetAmount());
        goal.setDeadline(newGoal.getDeadline());

        return goalRepository.save(goal);
    }
    // GET ONE GOAL
    public SavingsGoal getGoalById(Long id){
        return goalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
    }

}
