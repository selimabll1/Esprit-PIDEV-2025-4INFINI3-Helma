package tn.esprit.helma.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tn.esprit.helma.entities.Transaction;
import tn.esprit.helma.entities.User;
import tn.esprit.helma.repositories.UserRepository;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudAlertMailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${alert.mail.to:}")
    private String alertRecipient;

    @Value("${alert.mail.from:no-reply@helma.local}")
    private String alertSender;

    @Async
    public void sendSuspiciousTransactionAlert(Transaction transaction) {
        Integer risk = transaction != null ? transaction.getRiskScore() : null;

        if (risk == null || risk < 70) {
            log.info("Transaction {} risk={} : pas d'alerte (risque < 70)",
                    transaction != null ? transaction.getId() : null, risk);
            return;
        }

        // Charger l'email depuis la DB via userId (safe : pas de lazy loading)
        String ownerEmail = null;
        if (transaction.getBankAccount() != null && transaction.getBankAccount().getUserId() != null) {
            ownerEmail = userRepository.findById(transaction.getBankAccount().getUserId())
                    .map(User::getEmail)
                    .orElse(null);
        }

        String recipient = StringUtils.hasText(ownerEmail) ? ownerEmail : alertRecipient;

        if (!StringUtils.hasText(recipient)) {
            log.warn("Transaction {} : aucun destinataire disponible, envoi annulé.",
                    transaction.getId());
            return;
        }

        log.info("Alerte transaction #{} risk={} : envoi de {} vers {}",
                transaction.getId(), risk, alertSender, recipient);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(recipient);
            helper.setFrom(alertSender);
            helper.setSubject("[Helma] Confirmation de transaction requise #" + transaction.getId());
            helper.setText(buildTextBody(transaction), buildHtmlBody(transaction));
            mailSender.send(mimeMessage);
            log.info("E-mail d'alerte envoyé pour la transaction #{} à {}", transaction.getId(), recipient);
        } catch (Exception ex) {
            log.error("Échec d'envoi de l'e-mail d'alerte pour la transaction #{} — type: {} — raison: {}",
                    transaction.getId(),
                    ex.getClass().getSimpleName(),
                    ex.getMessage());
        }
    }

    private String buildTextBody(Transaction transaction) {
        BigDecimal amount = transaction.getAmount();
        return "⚠️ ALERTE TRANSACTION SUSPECTE ⚠️\n\n" +
                "Bonjour,\n\n" +
                "Une transaction de montant élevé a été détectée sur votre compte.\n\n" +
                "DÉTAILS DE LA TRANSACTION :\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "Montant : " + amount + " TND\n" +
                "Bénéficiaire : " + transaction.getBeneficiaryName() + "\n" +
                "RIB : " + transaction.getBeneficiaryRib() + "\n" +
                "Niveau de risque : " + transaction.getRiskScore() + "%\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "ACTION REQUISE :\n" +
                "Veuillez vous connecter à votre compte Helma et confirmer ou rejeter cette transaction.\n\n" +
                "⏰ Cette demande est valide pendant 24 heures.\n\n" +
                "Si vous ne reconnaissez pas cette transaction, rejetez-la immédiatement.\n\n" +
                "L'équipe Helma";
    }

    private String buildHtmlBody(Transaction transaction) {
        BigDecimal amount = transaction.getAmount();
        String beneficiaryRib = StringUtils.hasText(transaction.getBeneficiaryRib()) ? transaction.getBeneficiaryRib() : "—";
        Integer riskScore = transaction.getRiskScore();

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:\"Segoe UI\",Tahoma,Geneva,Verdana,sans-serif;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);margin:0;padding:20px;'>");
        sb.append("<div style='max-width:600px;margin:0 auto;'>");

        sb.append("<div style='background:#d64545;color:#fff;padding:20px;border-radius:12px 12px 0 0;text-align:center;'>");
        sb.append("<h1 style='margin:0;font-size:28px;'>⚠️ ALERTE TRANSACTION</h1>");
        sb.append("<p style='margin:8px 0 0 0;font-size:14px;'>Vérification de sécurité requise</p>");
        sb.append("</div>");

        sb.append("<div style='background:#fff;padding:30px;border-radius:0 0 12px 12px;box-shadow:0 10px 40px rgba(0,0,0,0.2);'>");
        sb.append("<p style='color:#333;margin-top:0;font-size:15px;line-height:1.6;'>Bonjour,<br><br>");
        sb.append("Une transaction de montant important a été détectée sur votre compte. ");
        sb.append("Par mesure de sécurité, nous vous demandons de la vérifier.</p>");

        sb.append("<div style='background:#f8f9fa;border-left:4px solid #667eea;padding:18px;margin:24px 0;border-radius:6px;'>");
        sb.append("<h3 style='margin-top:0;color:#667eea;font-size:16px;'>Détails de la transaction</h3>");
        sb.append("<table style='width:100%;border-collapse:collapse;'>");
        appendRow(sb, "Montant", "<span style='color:#d64545;font-weight:bold;font-size:18px;'>" + amount + " TND</span>");
        appendRow(sb, "Bénéficiaire", transaction.getBeneficiaryName());
        appendRow(sb, "RIB", beneficiaryRib);
        appendRow(sb, "Niveau de risque", riskScore + "%");
        sb.append("</table></div>");

        sb.append("<div style='background:#e8f5e9;border-left:4px solid #4caf50;padding:16px;margin:24px 0;border-radius:6px;'>");
        sb.append("<h3 style='margin-top:0;color:#2e7d32;font-size:15px;'>✓ Que faire ?</h3>");
        sb.append("<p style='margin:8px 0;color:#333;'><strong>1.</strong> Connectez-vous à votre compte Helma</p>");
        sb.append("<p style='margin:8px 0;color:#333;'><strong>2.</strong> Allez à votre tableau de bord transactions</p>");
        sb.append("<p style='margin:8px 0;color:#333;'><strong>3.</strong> Confirmez ou rejetez cette transaction</p>");
        if (riskScore != null && riskScore >= 70) {
            sb.append("<p style='margin:8px 0;color:#d64545;'><strong>🔐 Code PIN requis pour confirmer</strong></p>");
        }
        sb.append("</div>");

        sb.append("<div style='background:#fff3e0;border-left:4px solid #ff9800;padding:16px;margin:24px 0;border-radius:6px;'>");
        sb.append("<p style='margin:0;color:#e65100;font-size:14px;'><strong>⏰ Valide pendant 24 heures</strong><br>");
        sb.append("Si vous ne reconnaissez pas cette transaction, <strong>rejetez-la immédiatement</strong>.</p>");
        sb.append("</div>");

        sb.append("<div style='border-top:1px solid #eee;padding-top:16px;margin-top:24px;text-align:center;color:#666;font-size:12px;'>");
        sb.append("<p style='margin:8px 0;'>© 2026 Helma Banking - Tous droits réservés</p>");
        sb.append("</div>");

        sb.append("</div></div></body></html>");
        return sb.toString();
    }

    private void appendRow(StringBuilder sb, String label, String value) {
        sb.append("<tr style='border-top:1px solid #eef2f7;'><td style='padding:10px 0;color:#6b7280;width:40%;'>")
          .append(label)
          .append("</td><td style='padding:10px 0;text-align:right;color:#111827;font-weight:700;'>")
          .append(value == null ? "" : value)
          .append("</td></tr>");
    }
}
