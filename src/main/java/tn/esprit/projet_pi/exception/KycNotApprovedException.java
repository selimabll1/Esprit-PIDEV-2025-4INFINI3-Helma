package tn.esprit.projet_pi.exception;

public class KycNotApprovedException extends RuntimeException {
    public KycNotApprovedException(String message) {
        super(message);
    }

    public KycNotApprovedException(Long userId) {
        super("KYC not approved for user with id: " + userId);
    }
}
