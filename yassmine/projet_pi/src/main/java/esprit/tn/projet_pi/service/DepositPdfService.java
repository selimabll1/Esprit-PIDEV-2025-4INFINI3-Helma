package esprit.tn.projet_pi.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import esprit.tn.projet_pi.entity.SavingsDeposit;
import esprit.tn.projet_pi.entity.SavingsGoal;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;

@Service
public class DepositPdfService {

    public byte[] generateDepositsPdf(List<SavingsDeposit> deposits) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);

        try {

            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normal = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            DecimalFormat money = new DecimalFormat("#,##0.00");

            /* ================= HEADER ================= */

            Paragraph title = new Paragraph("SMART WALLET - SAVINGS STATEMENT")
                    .setFont(bold)
                    .setFontSize(18)
                    .setFontColor(ColorConstants.BLUE)
                    .setTextAlignment(TextAlignment.CENTER);

            document.add(title);
            document.add(new Paragraph("Generated on: " + LocalDate.now())
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10));

            document.add(new Paragraph("\n"));

            /* ================= TABLE ================= */

            float[] widths = {60, 90, 90, 90, 90, 80, 80};
            Table table = new Table(widths);
            table.setWidth(UnitValue.createPercentValue(100));

            // header row
            table.addHeaderCell(header("ID"));
            table.addHeaderCell(header("User"));
            table.addHeaderCell(header("Goal"));
            table.addHeaderCell(header("Deposit Date"));
            table.addHeaderCell(header("Amount"));
            table.addHeaderCell(header("Progress"));
            table.addHeaderCell(header("Status"));

            double total = 0;

            /* ================= JOIN LOGIC ================= */

            for (SavingsDeposit d : deposits) {

                SavingsGoal goal = d.getSavingsGoal();

                String userName = goal.getUser().getName();
                String goalTitle = goal.getTitle();

                double progress = (goal.getCurrentAmount() / goal.getTargetAmount()) * 100;

                total += d.getAmount();

                table.addCell(cell(String.valueOf(d.getId())));
                table.addCell(cell(userName));
                table.addCell(cell(goalTitle));
                table.addCell(cell(d.getDateDeposit().toString()));
                table.addCell(cell(money.format(d.getAmount()) + " TND"));

                // progress %
                //* progress (%) = (montant actuel / montant cible) × 100
                Cell progressCell = cell(String.format("%.1f %%", progress));
                progressCell.setTextAlignment(TextAlignment.RIGHT);
                table.addCell(progressCell);

                // status color
                Cell statusCell = cell(goal.getStatus().name());

                if(goal.getStatus().name().equals("ACHIEVED"))
                    statusCell.setFontColor(ColorConstants.GREEN);
                else
                    statusCell.setFontColor(ColorConstants.ORANGE);

                table.addCell(statusCell);
            }

            document.add(table);

            document.add(new Paragraph("\n"));

            /* ================= SUMMARY ================= */

            Paragraph totalLine = new Paragraph(
                    "Total Deposited: " + money.format(total) + " TND")
                    .setFont(bold)
                    .setFontSize(13)
                    .setTextAlignment(TextAlignment.RIGHT);

            document.add(totalLine);

            document.add(new Paragraph("\n"));

            /* ================= FOOTER ================= */

            document.add(new Paragraph(
                    "This document is an official digital savings statement generated automatically by Smart Wallet system.")
                    .setFontSize(9)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER));

            document.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return out.toByteArray();
    }

    /* ===== helpers ===== */
    //* Crée une cellule d'en-tête pour un tableau PDF.
    // * - Texte en gras
    // * - Fond gris clair
    // * - Texte centré
    // * - Padding pour meilleure lisibilité
    private Cell header(String text){
        return new Cell()
                .add(new Paragraph(text))
                .setBold()
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(6);
    }

    private Cell cell(String text){
        return new Cell()
                .add(new Paragraph(text))
                .setPadding(5)
                .setFontSize(10);
    }
}