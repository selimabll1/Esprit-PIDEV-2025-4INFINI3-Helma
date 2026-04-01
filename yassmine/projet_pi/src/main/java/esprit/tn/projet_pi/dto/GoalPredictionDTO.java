package esprit.tn.projet_pi.dto;

public class GoalPredictionDTO {

    private String goalTitle;
    private double targetAmount;
    private double currentAmount;
    private double remainingAmount;
    private long daysRemaining;
    private double requiredPerDay;
    private double actualPerDay;
    private String prediction;
    private String message;

    public GoalPredictionDTO(String goalTitle, double targetAmount, double currentAmount,
                             double remainingAmount, long daysRemaining, double requiredPerDay,
                             double actualPerDay, String prediction, String message) {
        this.goalTitle = goalTitle;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        //* le  remaining montant qu’il reste à économiser
       //* targetAmount - currentAmount
        this.remainingAmount = remainingAmount;
        //*combien l’utilisateur doit économiser chaque jour
        //*remainingAmount / daysRemaining
        this.daysRemaining = daysRemaining;
        this.requiredPerDay = requiredPerDay;
        //*son rythme d’épargne totalDeposits / daysSinceCreation
        this.actualPerDay = actualPerDay;
        //*if (actualPerDay >= requiredPerDay)
        //    → WILL_ACHIEVE
        //else
        //    → WILL_NOT_ACHIEVE
        this.prediction = prediction;
        this.message = message;
    }

    public String getGoalTitle() { return goalTitle; }
    public double getTargetAmount() { return targetAmount; }
    public double getCurrentAmount() { return currentAmount; }
    public double getRemainingAmount() { return remainingAmount; }
    public long getDaysRemaining() { return daysRemaining; }
    public double getRequiredPerDay() { return requiredPerDay; }
    public double getActualPerDay() { return actualPerDay; }
    public String getPrediction() { return prediction; }
    public String getMessage() { return message; }
}