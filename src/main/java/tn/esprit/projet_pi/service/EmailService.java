package tn.esprit.projet_pi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import tn.esprit.projet_pi.entity.Loan;
import tn.esprit.projet_pi.entity.LoanPayment;
import tn.esprit.projet_pi.entity.RepaymentSchedule;
import tn.esprit.projet_pi.repository.RepaymentScheduleRepository;

import java.util.Optional;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final UserEmailResolver userEmailResolver;
    private final RepaymentScheduleRepository repaymentScheduleRepository;

    @Value("${helma.admin.email:aminenadia007@gmail.com}")
    private String adminEmail;

    public EmailService(JavaMailSender mailSender,
            UserEmailResolver userEmailResolver,
            RepaymentScheduleRepository repaymentScheduleRepository) {
        this.mailSender = mailSender;
        this.userEmailResolver = userEmailResolver;
        this.repaymentScheduleRepository = repaymentScheduleRepository;
    }

    public void sendLoanCreated(Loan loan) {
        sendToBorrower(loan, "HELMA - Votre demande de pret est en cours",
                "Bonjour,\n\nNous confirmons la reception de votre demande de pret HELMA.\n"
                        + "Montant demande : " + loan.getPrincipalAmount() + " TND\n"
                        + "Duree : " + loan.getDurationMonths() + " mois\n\n"
                        + "Votre dossier est en cours d'analyse. Nous vous tiendrons informe rapidement.\n\nHELMA");
    }

    public void sendLoanApproved(Loan loan, byte[] pdfAttachment) {
        try {
            Optional<String> email = userEmailResolver.resolveEmail(loan.getUserId());
            if (email.isEmpty()) return;
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email.get());
            helper.setSubject("HELMA - Felicitations, votre pret est approuve !");
            helper.setText("Bonjour,\n\nVotre pret est approuve.\nMontant approuve : "
                    + loan.getPrincipalAmount() + " TND\nMensualite : " + loan.getMonthlyPayment()
                    + "\nPremiere echeance : " + nextDueDate(loan)
                    + "\n\nVotre contrat est joint a cet email.\n\nHELMA");
            helper.addAttachment("contrat_" + loan.getId() + ".pdf", new ByteArrayResource(pdfAttachment));
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Erreur email approbation pret {}: {}", loan.getId(), e.getMessage(), e);
        }
    }

    public void sendLoanRejected(Loan loan, String reason) {
        sendToBorrower(loan, "HELMA - Votre demande de pret a ete refusee",
                "Bonjour,\n\nVotre demande de pret a ete refusee.\nMotif : " + reason
                        + "\n\nConseils : regularisez les paiements en attente, reduisez votre endettement et deposez un dossier avec une duree adaptee.\n\nHELMA");
    }

    public void sendPaymentReminder(Loan loan, LoanPayment nextPayment) {
        sendToBorrower(loan, "HELMA - Rappel : echeance dans 3 jours",
                "Bonjour,\n\nRappel de paiement a venir.\nMontant du : " + nextPayment.getAmount()
                        + " TND\nDate echeance : " + nextPayment.getPaidAt()
                        + "\n\nMerci d'effectuer le paiement via votre espace HELMA.\n\nHELMA");
    }

    public void sendOverdueAlert(Loan loan, LoanPayment overduePayment) {
        sendToBorrower(loan, "HELMA - Paiement en retard",
                "Bonjour,\n\nUn paiement est en retard.\nMontant : " + overduePayment.getAmount()
                        + " TND\nDate : " + overduePayment.getPaidAt()
                        + "\n\nCe retard peut affecter votre score HELMA. Contactez notre support pour regulariser la situation.\n\nHELMA");
    }

    public void sendDefaultAlert(Loan loan) {
        sendToBorrower(loan, "HELMA - Alerte : pret en defaut",
                "Bonjour,\n\nVotre pret est actuellement en defaut. Une regularisation urgente est necessaire.\n"
                        + "Merci de contacter l'equipe HELMA afin de convenir des demarches a effectuer.\n\nHELMA");
    }

    public void sendAdminEarlyWarning(Loan loan, String explanation) {
        send(adminEmail, "HELMA - Alerte Early Warning pret #" + loan.getId(),
                "Pret a risque detecte.\nLoanId : " + loan.getId()
                        + "\nUserId : " + loan.getUserId()
                        + "\nExplication IA : " + explanation);
    }

    private void sendToBorrower(Loan loan, String subject, String body) {
        userEmailResolver.resolveEmail(loan.getUserId()).ifPresent(email -> send(email, subject, body));
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Erreur envoi email '{}': {}", subject, e.getMessage(), e);
        }
    }

    private String nextDueDate(Loan loan) {
        return repaymentScheduleRepository.findNextPendingByLoanId(loan.getId())
                .map(RepaymentSchedule::getDueDate)
                .map(String::valueOf)
                .orElse("a confirmer");
    }
}
