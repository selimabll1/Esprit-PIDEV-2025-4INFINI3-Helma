package tn.esprit.projet_pi.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import tn.esprit.projet_pi.entity.Loan;
import tn.esprit.projet_pi.entity.RepaymentSchedule;
import tn.esprit.projet_pi.repository.RepaymentScheduleRepository;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class PdfContractService {

    private final RepaymentScheduleRepository repaymentScheduleRepository;

    public PdfContractService(RepaymentScheduleRepository repaymentScheduleRepository) {
        this.repaymentScheduleRepository = repaymentScheduleRepository;
    }

    public byte[] generateContract(Loan loan) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream content = new PDPageContentStream(document, page);
            float y = 780;
            y = line(content, "HELMA - CONTRAT DE PRET", 50, y, 18, true);
            y = line(content, "Date de generation : " + LocalDate.now(), 50, y - 8, 11, false);
            y = line(content, "Informations emprunteur", 50, y - 12, 14, true);
            y = line(content, "UserId : " + loan.getUserId() + " | Type : " + loan.getLoanType(), 50, y, 11, false);
            y = line(content, "Details pret", 50, y - 12, 14, true);
            y = line(content, "Montant : " + loan.getPrincipalAmount() + " TND | Taux : " + loan.getInterestRate() + "%", 50, y, 11, false);
            y = line(content, "Duree : " + loan.getDurationMonths() + " mois | Mensualite : " + loan.getMonthlyPayment(), 50, y, 11, false);
            y = line(content, "Score de risque : " + loan.getRiskScore() + " | Decision IA : " + loan.getStatus(), 50, y, 11, false);
            y = line(content, "Tableau des echeances", 50, y - 12, 14, true);
            y = line(content, "No.     Date              Montant              Statut", 50, y, 11, true);

            List<RepaymentSchedule> schedules = repaymentScheduleRepository.findByLoanIdOrdered(loan.getId());
            for (RepaymentSchedule schedule : schedules) {
                if (y < 90) {
                    content.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    y = 780;
                }
                y = line(content, String.format("%-7s %-17s %-20s %s",
                        schedule.getInstallmentNumber(),
                        schedule.getDueDate(),
                        schedule.getExpectedAmount(),
                        schedule.getStatus()), 50, y, 10, false);
            }

            y = line(content, "Conditions generales du pret", 50, y - 12, 14, true);
            y = line(content, "L'emprunteur s'engage a payer chaque echeance a la date prevue.", 50, y, 10, false);
            y = line(content, "Tout retard peut entrainer des frais, une alerte risque et une revue du dossier.", 50, y, 10, false);
            line(content, "Signature electronique simulee : HELMA-" + loan.getId() + "-" + LocalDate.now(), 50, y - 12, 11, true);
            content.close();
            document.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Erreur generation PDF contrat pret {}: {}", loan.getId(), e.getMessage(), e);
            throw new RuntimeException("Impossible de generer le contrat PDF: " + e.getMessage());
        }
    }

    private float line(PDPageContentStream content, String text, float x, float y, int size, boolean bold) throws Exception {
        content.beginText();
        content.setFont(new PDType1Font(bold
                ? Standard14Fonts.FontName.HELVETICA_BOLD
                : Standard14Fonts.FontName.HELVETICA), size);
        content.newLineAtOffset(x, y);
        content.showText(text == null ? "" : text);
        content.endText();
        return y - 17;
    }
}
