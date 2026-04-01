package esprit.tn.projet_pi.dto;

public class DepositStatsDTO {

    private long totalDeposits;
    private double totalAmount;
    private double averageAmount;
    private double maxAmount;
    private double minAmount;

    public DepositStatsDTO(long totalDeposits, double totalAmount,
                           double averageAmount, double maxAmount, double minAmount) {
        this.totalDeposits = totalDeposits;
        this.totalAmount = totalAmount;
        this.averageAmount = averageAmount;
        this.maxAmount = maxAmount;
        this.minAmount = minAmount;
    }

    public long getTotalDeposits() { return totalDeposits; }
    public double getTotalAmount() { return totalAmount; }
    public double getAverageAmount() { return averageAmount; }
    public double getMaxAmount() { return maxAmount; }
    public double getMinAmount() { return minAmount; }
}
