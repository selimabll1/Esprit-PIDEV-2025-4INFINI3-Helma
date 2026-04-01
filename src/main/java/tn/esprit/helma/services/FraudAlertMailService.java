package tn.esprit.helma.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tn.esprit.helma.entities.Transaction;

import java.math.BigDecimal;

/**
 * Service responsable des notifications e-mail liées aux transactions suspectes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudAlertMailService {

    private final JavaMailSender mailSender;

    @Value("${alert.mail.to:}")
    private String alertRecipient;

    @Value("${alert.mail.from:no-reply@helma.local}")
    private String alertSender;

    @Value("${alert.mail.confirm-base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Envoie un e-mail unique pour confirmer ou rejeter une transaction suspecte.
     */
    public void sendSuspiciousTransactionAlert(Transaction transaction) {
        if (!StringUtils.hasText(alertRecipient)) {
            log.warn("Aucun destinataire configuré pour les alertes e-mail, envoi annulé.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(alertRecipient);
            message.setFrom(alertSender);
            message.setSubject("[Helma] Confirmation transaction suspecte #" + transaction.getId());
            message.setText(buildBody(transaction));
            mailSender.send(message);
            log.info("E-mail d'alerte envoyé pour la transaction {}", transaction.getId());
        } catch (Exception ex) {
            log.error("Échec d'envoi de l'e-mail d'alerte pour la transaction {}", transaction.getId(), ex);
        }
    }

    private String buildBody(Transaction transaction) {
        String confirmUrl = String.format("%s/transactions/%d/confirm", baseUrl, transaction.getId());
        String rejectUrl = String.format("%s/transactions/%d/reject", baseUrl, transaction.getId());
        BigDecimal amount = transaction.getAmount();
        return "Bonjour,\n\n" +
                "Une transaction suspecte a été détectée :\n" +
                "- Montant : " + amount + " TND\n" +
                "- Bénéficiaire : " + transaction.getBeneficiaryName() + " (" + transaction.getBeneficiaryRib() + ")\n" +
                "- Score de risque : " + transaction.getRiskScore() + "\n\n" +
                "Merci de confirmer qu'il s'agit bien de vous :\n" +
                "Confirmer : " + confirmUrl + "\n" +
                "Rejeter : " + rejectUrl + "\n\n" +
                "L'équipe Helma";
    }
}
