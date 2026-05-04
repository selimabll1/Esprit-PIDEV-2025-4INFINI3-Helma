package com.helma.helmabackend.service;

import com.helma.helmabackend.entity.SavingsDeposit;
import com.helma.helmabackend.entity.SavingsGoal;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.RoundDotsBorder;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DepositPdfService {

    /* ══════════════════════════════════════════════════════════════
       HELMA BRAND COLORS  (from frontend CSS)
       Gold      #D4A62A
       Teal Dark #0F6B6B
       Teal Mid  #0B4C4C
       Mint      #DDF4EC
       Ivory     #FCFAF5
       Text      #1F2937
       Muted     #6B7280
       White     #FFFFFF
       ══════════════════════════════════════════════════════════════ */

    private static final Color TEAL_DARK  = hex("#0F6B6B");
    private static final Color TEAL_MID   = hex("#0B4C4C");
    private static final Color TEAL_LIGHT = hex("#DDF4EC");
    private static final Color GOLD       = hex("#D4A62A");
    private static final Color GOLD_LIGHT = hex("#FDF3DC");
    private static final Color IVORY      = hex("#FCFAF5");
    private static final Color BG         = hex("#F8FAFC");
    private static final Color TEXT_DARK  = hex("#1F2937");
    private static final Color TEXT_MUTED = hex("#6B7280");
    private static final Color BORDER     = hex("#E6E8EC");
    private static final Color WHITE      = new DeviceRgb(255, 255, 255);

    /* ──────────────────────────────────────────────────────────────
       PUBLIC ENTRY POINT
       ────────────────────────────────────────────────────────────── */

    public byte[] generateDepositsPdf(List<SavingsDeposit> deposits) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter  pdfWriter  = new PdfWriter(out);
            PdfDocument pdfDoc   = new PdfDocument(pdfWriter);
            Document   document  = new Document(pdfDoc, PageSize.A4);
            document.setMargins(0, 0, 36, 0);

            PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normal  = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont oblique = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            DecimalFormat money = new DecimalFormat("#,##0.00");
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy");

            float pageW = PageSize.A4.getWidth();

            // ── Grab goal-level metadata from first deposit ──────────
            SavingsGoal  goal     = deposits.isEmpty() ? null : deposits.get(0).getSavingsGoal();
            String       goalTitle = goal != null ? goal.getTitle() : "Savings Goal";
            String       userName  = goal != null
                    ? goal.getUser().getProfile().getFirstName() + " "
                    + goal.getUser().getProfile().getLastName()
                    : "—";
            double       target    = goal != null ? goal.getTargetAmount()   : 0;
            double       current   = goal != null ? goal.getCurrentAmount()  : 0;
            double       progress  = target > 0 ? (current / target) * 100  : 0;
            LocalDate    created   = goal != null ? goal.getCreationDate()   : LocalDate.now();
            LocalDate    deadline  = goal != null ? goal.getDeadline()       : null;
            String       status    = goal != null ? goal.getStatus().name()  : "—";

            double total = deposits.stream().mapToDouble(SavingsDeposit::getAmount).sum();

            // ════════════════════════════════════════════════════════
            //  HEADER BAND  (drawn on canvas for gradient effect)
            // ════════════════════════════════════════════════════════
            PdfPage   firstPage = pdfDoc.addNewPage();
            PdfCanvas pc        = new PdfCanvas(firstPage);
            float     headerH   = 210f;
            float     pageH     = PageSize.A4.getHeight();

            // Background: teal gradient (simulate with two rects)
            pc.setFillColor(TEAL_DARK);
            pc.rectangle(0, pageH - headerH, pageW, headerH);
            pc.fill();

            // Subtle gold orb (top-right decorative circle)
            pc.saveState();
            PdfExtGState gs = new PdfExtGState().setFillOpacity(0.12f);
            pc.setExtGState(gs);
            pc.setFillColor(GOLD);
            pc.circle(pageW - 60, pageH - 30, 160);
            pc.fill();
            pc.restoreState();

            // Gold bottom accent line
            pc.setFillColor(GOLD);
            pc.rectangle(0, pageH - headerH, pageW, 3f);
            pc.fill();

            // Left teal-mid accent stripe
            pc.setFillColor(TEAL_MID);
            pc.rectangle(0, pageH - headerH, 6f, headerH);
            pc.fill();

            pc.release();

            // ── Header text via Canvas ───────────────────────────────
            Canvas hCanvas = new Canvas(new PdfCanvas(firstPage), new Rectangle(0, pageH - headerH, pageW, headerH));

            // App name
            hCanvas.add(new Paragraph("HELMA")
                    .setFont(bold).setFontSize(32).setFontColor(WHITE)
                    .setFixedPosition(20, pageH - 58, 200)
                    .setMargin(0));

            // Tagline
            hCanvas.add(new Paragraph("Smart Wallet  ·  Intelligent Savings Management")
                    .setFont(normal).setFontSize(9)
                    .setFontColor(new DeviceRgb(180, 220, 220))
                    .setFixedPosition(20, pageH - 76, 300)
                    .setMargin(0));

            // Goal title (large)
            hCanvas.add(new Paragraph(goalTitle.toUpperCase())
                    .setFont(bold).setFontSize(11)
                    .setFontColor(new DeviceRgb(180, 220, 220))
                    .setFixedPosition(20, pageH - 108, 300)
                    .setMargin(0));

            // Total amount (huge)
            hCanvas.add(new Paragraph(money.format(total) + " TND")
                    .setFont(bold).setFontSize(40)
                    .setFontColor(WHITE)
                    .setFixedPosition(20, pageH - 158, 360)
                    .setMargin(0));

            // Sub-label
            hCanvas.add(new Paragraph("ACCOUNT STATEMENT")
                    .setFont(bold).setFontSize(8)
                    .setFontColor(new DeviceRgb(180, 220, 220))
                    .setCharacterSpacing(2f)
                    .setFixedPosition(20, pageH - 175, 200)
                    .setMargin(0));

            // ── Progress badge (right side) ──────────────────────────
            float badgeX = pageW - 120;
            float badgeY = pageH - 90;

            PdfCanvas pc2 = new PdfCanvas(firstPage);
            pc2.setFillColor(GOLD);
            pc2.roundRectangle(badgeX, badgeY - 42, 90, 42, 12);
            pc2.fill();
            pc2.release();

            hCanvas.add(new Paragraph(String.format("%.0f%%", progress))
                    .setFont(bold).setFontSize(26).setFontColor(WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFixedPosition(badgeX, badgeY - 35, 90)
                    .setMargin(0));

            hCanvas.add(new Paragraph("Progress")
                    .setFont(normal).setFontSize(8)
                    .setFontColor(new DeviceRgb(220, 220, 220))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setFixedPosition(badgeX, badgeY + 2, 90)
                    .setMargin(0));

            // Dates (right side lower)
            hCanvas.add(new Paragraph("All your savings deposits")
                    .setFont(oblique).setFontSize(9)
                    .setFontColor(new DeviceRgb(180, 220, 220))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setFixedPosition(pageW - 200, pageH - 120, 180)
                    .setMargin(0));

            // Created date
            hCanvas.add(new Paragraph("Created: " + (created != null ? created.format(fmt) : "—"))
                    .setFont(normal).setFontSize(8.5f)
                    .setFontColor(new DeviceRgb(200, 230, 230))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setFixedPosition(pageW - 220, pageH - 155, 200)
                    .setMargin(0));

            // Deadline
            if (deadline != null) {
                hCanvas.add(new Paragraph("Deadline: " + deadline.format(fmt))
                        .setFont(normal).setFontSize(8.5f)
                        .setFontColor(new DeviceRgb(200, 230, 230))
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setFixedPosition(pageW - 220, pageH - 170, 200)
                        .setMargin(0));
            }

            hCanvas.close();

            // ════════════════════════════════════════════════════════
            //  INFO CARDS ROW  (2 cards side by side)
            // ════════════════════════════════════════════════════════
            document.add(new Paragraph("").setMarginTop(headerH + 18));

            float cardW = (pageW - 60 - 12) / 2f;
            Table infoRow = new Table(new float[]{cardW, cardW})
                    .setWidth(pageW - 60)
                    .setMarginLeft(20)
                    .setMarginBottom(24);

            // Card 1: Account holder info
            Cell card1 = new Cell()
                    .setBorder(new SolidBorder(BORDER, 0.8f))
                    .setBackgroundColor(WHITE)
                    .setBorderRadius(new BorderRadius(14))
                    .setPadding(16)
                    .setMarginRight(6);
            card1.add(infoLabel("ACCOUNT HOLDER", bold, normal));
            card1.add(infoValue(userName, bold));
            card1.add(new Paragraph(" ").setFontSize(4));
            card1.add(infoLabel("GOAL", bold, normal));
            card1.add(infoValue(goalTitle, bold));
            card1.add(new Paragraph(" ").setFontSize(4));
            card1.add(infoLabel("TARGET AMOUNT", bold, normal));
            card1.add(infoValue(money.format(target) + " TND", bold));
            infoRow.addCell(card1);

            // Card 2: Statement info
            Cell card2 = new Cell()
                    .setBorder(new SolidBorder(BORDER, 0.8f))
                    .setBackgroundColor(IVORY)
                    .setBorderRadius(new BorderRadius(14))
                    .setPadding(16)
                    .setMarginLeft(6);
            card2.add(infoLabel("STATEMENT DATE", bold, normal));
            card2.add(infoValue(LocalDate.now().format(fmt), bold));
            card2.add(new Paragraph(" ").setFontSize(4));
            card2.add(infoLabel("STATUS", bold, normal));

            boolean achieved = "ACHIEVED".equalsIgnoreCase(status);
            Paragraph statusPara = new Paragraph(achieved ? "ACHIEVED" : "IN PROGRESS")
                    .setFont(bold).setFontSize(10)
                    .setFontColor(achieved ? TEAL_DARK : GOLD);
            card2.add(statusPara);

            card2.add(new Paragraph(" ").setFontSize(4));
            card2.add(infoLabel("TOTAL DEPOSITED", bold, normal));
            card2.add(infoValue(money.format(total) + " TND", bold));
            infoRow.addCell(card2);

            document.add(infoRow);

            // ════════════════════════════════════════════════════════
            //  SECTION TITLE — TRANSACTION HISTORY
            // ════════════════════════════════════════════════════════
            document.add(sectionTitle("DEPOSIT HISTORY", bold, normal));

            // ════════════════════════════════════════════════════════
            //  TRANSACTION TABLE
            // ════════════════════════════════════════════════════════
            float usable = pageW - 40;
            float[] cols = {36, 90, 90, 90, 100, 76, 76};
            Table table = new Table(cols)
                    .setWidth(usable)
                    .setMarginLeft(20)
                    .setMarginBottom(20);

            // Header row
            String[] headers = {"#", "Account Holder", "Goal", "Deposit Date", "Amount (TND)", "Progress", "Status"};
            for (String h : headers) {
                table.addHeaderCell(
                        new Cell()
                                .add(new Paragraph(h).setFont(bold).setFontSize(8).setFontColor(WHITE))
                                .setBackgroundColor(TEAL_DARK)
                                .setTextAlignment(TextAlignment.CENTER)
                                .setPaddingTop(9).setPaddingBottom(9)
                                .setBorder(Border.NO_BORDER)
                );
            }

            // Data rows
            boolean alt = false;
            for (SavingsDeposit d : deposits) {
                SavingsGoal g = d.getSavingsGoal();
                String uName  = g.getUser().getProfile().getFirstName() + " " + g.getUser().getProfile().getLastName();
                String gTitle = g.getTitle();
                double prog   = g.getTargetAmount() > 0 ? (g.getCurrentAmount() / g.getTargetAmount()) * 100 : 0;
                boolean done  = "ACHIEVED".equalsIgnoreCase(g.getStatus().name());
                Color rowBg   = alt ? new DeviceRgb(248, 250, 252) : WHITE;

                table.addCell(dataCell(String.valueOf(d.getId()), normal, rowBg, TextAlignment.CENTER));
                table.addCell(dataCell(uName, normal, rowBg, TextAlignment.LEFT));
                table.addCell(goalCell(gTitle, bold, rowBg));
                table.addCell(dataCell(d.getDateDeposit().toString(), normal, rowBg, TextAlignment.CENTER));
                table.addCell(dataCell(money.format(d.getAmount()), bold, rowBg, TextAlignment.RIGHT));
                table.addCell(progressCell(prog, bold, normal, rowBg));
                table.addCell(statusCell(done, bold, rowBg));

                alt = !alt;
            }

            document.add(table);

            // ════════════════════════════════════════════════════════
            //  SUMMARY BY GOAL CARDS
            // ════════════════════════════════════════════════════════
            // Group deposits by goal title
            Map<String, double[]> byGoal = new LinkedHashMap<>();
            for (SavingsDeposit d : deposits) {
                String key = d.getSavingsGoal().getTitle();
                byGoal.computeIfAbsent(key, k -> new double[]{0, 0})[0] += d.getAmount();
                byGoal.get(key)[1]++;
            }

            if (byGoal.size() > 1) {
                document.add(sectionTitle("SUMMARY BY GOAL", bold, normal));

                int numCards = byGoal.size();
                float sumCardW = (usable - (numCards - 1) * 10f) / numCards;
                Table summaryRow = new Table(new float[numCards])
                        .setWidth(usable).setMarginLeft(20).setMarginBottom(20);

                Color[] cardColors = {TEAL_DARK, TEAL_MID, hex("#1A7A7A"), hex("#0A5555")};
                int ci = 0;
                for (Map.Entry<String, double[]> e : byGoal.entrySet()) {
                    Color bg = cardColors[ci % cardColors.length];
                    Cell sc = new Cell()
                            .setBackgroundColor(bg)
                            .setBorderRadius(new BorderRadius(14))
                            .setPadding(16)
                            .setBorder(Border.NO_BORDER)
                            .setTextAlignment(TextAlignment.CENTER);

                    sc.add(new Paragraph(e.getKey().toUpperCase())
                            .setFont(bold).setFontSize(10).setFontColor(WHITE)
                            .setTextAlignment(TextAlignment.CENTER));
                    sc.add(new Paragraph(money.format(e.getValue()[0]) + " TND")
                            .setFont(bold).setFontSize(14).setFontColor(WHITE)
                            .setTextAlignment(TextAlignment.CENTER).setMarginTop(4));
                    sc.add(new Paragraph((int) e.getValue()[1] + " deposit(s)")
                            .setFont(normal).setFontSize(8)
                            .setFontColor(new DeviceRgb(180, 220, 220))
                            .setTextAlignment(TextAlignment.CENTER));

                    summaryRow.addCell(sc);
                    ci++;
                }
                document.add(summaryRow);
            }

            // ════════════════════════════════════════════════════════
            //  TOTAL BAND
            // ════════════════════════════════════════════════════════
            Table totalBand = new Table(new float[]{usable})
                    .setWidth(usable)
                    .setMarginLeft(20)
                    .setMarginBottom(16);

            Cell totalCell = new Cell()
                    .setBackgroundColor(GOLD_LIGHT)
                    .setBorder(new SolidBorder(GOLD, 1.5f))
                    .setBorderRadius(new BorderRadius(12))
                    .setPaddingTop(14).setPaddingBottom(14)
                    .setPaddingLeft(20).setPaddingRight(20);

            Paragraph totalPara = new Paragraph()
                    .add(new Text("Total Deposited:  ").setFont(normal).setFontSize(12).setFontColor(TEXT_DARK))
                    .add(new Text(money.format(total) + " TND").setFont(bold).setFontSize(16).setFontColor(TEAL_DARK))
                    .setTextAlignment(TextAlignment.RIGHT);

            totalCell.add(totalPara);
            totalBand.addCell(totalCell);
            document.add(totalBand);

            // ════════════════════════════════════════════════════════
            //  FOOTER NOTE
            // ════════════════════════════════════════════════════════
            document.add(new Paragraph(
                    "This document is an official digital savings statement generated automatically by the Helma Smart Wallet system. "
                            + "It reflects the state of your savings as of the date indicated. "
                            + "For any enquiries, please contact your Helma advisor.")
                    .setFont(oblique).setFontSize(8).setFontColor(TEXT_MUTED)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginLeft(20).setMarginRight(20).setMarginTop(8)
                    .setBorderTop(new SolidBorder(BORDER, 0.5f))
                    .setPaddingTop(10));

            document.close();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return out.toByteArray();
    }

    /* ══════════════════════════════════════════════════════════════
       PRIVATE HELPERS
       ══════════════════════════════════════════════════════════════ */

    private static Paragraph infoLabel(String text, PdfFont bold, PdfFont normal) {
        return new Paragraph(text)
                .setFont(bold).setFontSize(7.5f)
                .setFontColor(TEXT_MUTED)
                .setCharacterSpacing(1f)
                .setMarginBottom(2).setMarginTop(0);
    }

    private static Paragraph infoValue(String text, PdfFont bold) {
        return new Paragraph(text)
                .setFont(bold).setFontSize(11)
                .setFontColor(TEXT_DARK)
                .setMarginBottom(0).setMarginTop(0);
    }

    private static Div sectionTitle(String text, PdfFont bold, PdfFont normal) {
        Div div = new Div()
                .setMarginLeft(20).setMarginBottom(8).setMarginTop(4);
        div.add(new Paragraph(text)
                .setFont(bold).setFontSize(10)
                .setFontColor(TEAL_DARK)
                .setMarginBottom(4));
        return div;
    }

    private static Cell dataCell(String text, PdfFont font, Color bg, TextAlignment align) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(9).setFontColor(TEXT_DARK))
                .setBackgroundColor(bg)
                .setTextAlignment(align)
                .setPaddingTop(8).setPaddingBottom(8)
                .setPaddingLeft(7).setPaddingRight(7)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderTop(new SolidBorder(BORDER, 0.4f))
                .setBorderBottom(new SolidBorder(BORDER, 0.4f));
    }

    private static Cell goalCell(String text, PdfFont bold, Color bg) {
        return new Cell()
                .add(new Paragraph(text).setFont(bold).setFontSize(9).setFontColor(TEAL_DARK))
                .setBackgroundColor(bg)
                .setTextAlignment(TextAlignment.CENTER)
                .setPaddingTop(8).setPaddingBottom(8)
                .setPaddingLeft(7).setPaddingRight(7)
                .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                .setBorderTop(new SolidBorder(BORDER, 0.4f))
                .setBorderBottom(new SolidBorder(BORDER, 0.4f));
    }

    private static Cell progressCell(double prog, PdfFont bold, PdfFont normal, Color bg) {
        Color color = prog >= 100 ? TEAL_DARK : (prog >= 50 ? GOLD : TEXT_MUTED);
        return new Cell()
                .add(new Paragraph(String.format("%.1f%%", prog))
                        .setFont(bold).setFontSize(9).setFontColor(color))
                .setBackgroundColor(bg)
                .setTextAlignment(TextAlignment.CENTER)
                .setPaddingTop(8).setPaddingBottom(8)
                .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                .setBorderTop(new SolidBorder(BORDER, 0.4f))
                .setBorderBottom(new SolidBorder(BORDER, 0.4f));
    }

    private static Cell statusCell(boolean achieved, PdfFont bold, Color bg) {
        Color color = achieved ? TEAL_DARK : GOLD;
        String label = achieved ? "ACHIEVED" : "IN PROGRESS";
        return new Cell()
                .add(new Paragraph(label).setFont(bold).setFontSize(8).setFontColor(color))
                .setBackgroundColor(bg)
                .setTextAlignment(TextAlignment.CENTER)
                .setPaddingTop(8).setPaddingBottom(8)
                .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                .setBorderTop(new SolidBorder(BORDER, 0.4f))
                .setBorderBottom(new SolidBorder(BORDER, 0.4f));
    }

    /** Convert hex color string to iText DeviceRgb */
    private static DeviceRgb hex(String hex) {
        hex = hex.replace("#", "");
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return new DeviceRgb(r, g, b);
    }
}