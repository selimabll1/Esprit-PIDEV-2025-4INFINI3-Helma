package tn.esprit.projet_pi.exception;

public class LoanAlreadyActiveException extends RuntimeException {
    public LoanAlreadyActiveException(String message) {
        super(message);
    }

    public LoanAlreadyActiveException(Long loanId) {
        super("Loan is already active with id: " + loanId);
    }
}
