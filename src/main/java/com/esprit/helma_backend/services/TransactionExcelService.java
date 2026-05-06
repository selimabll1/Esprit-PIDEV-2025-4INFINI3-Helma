package com.esprit.helma_backend.services;

import com.esprit.helma_backend.entities.Transaction;
import com.esprit.helma_backend.repositories.TransactionRepository;
import com.esprit.helma_backend.repositories.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TransactionExcelService {

    private final TransactionRepository txRepo;
    private final UserRepository userRepo;

    public TransactionExcelService(TransactionRepository txRepo, UserRepository userRepo) {
        this.txRepo = txRepo;
        this.userRepo = userRepo;
    }

    public byte[] generate(Long userId, LocalDate month) {
        LocalDate monthStart = month.withDayOfMonth(1);
        ZoneId zone = ZoneId.systemDefault();
        Instant from = monthStart.atStartOfDay(zone).toInstant();
        Instant to = monthStart.plusMonths(1).atStartOfDay(zone).toInstant();

        var user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Transaction> txs = txRepo.findByUser(user).stream()
                .filter(t -> t.getTxnDate() != null && !t.getTxnDate().isBefore(from) && t.getTxnDate().isBefore(to))
                .sorted((a, b) -> a.getTxnDate().compareTo(b.getTxnDate()))
                .toList();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Transactions");

            // ── Styles ──────────────────────────────
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle titleStyle = wb.createCellStyle();
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(wb.createDataFormat().getFormat("dd/mm/yyyy"));

            CellStyle moneyStyle = wb.createCellStyle();
            moneyStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

            CellStyle incomeStyle = wb.createCellStyle();
            Font incomeFont = wb.createFont();
            incomeFont.setColor(IndexedColors.GREEN.getIndex());
            incomeFont.setBold(true);
            incomeStyle.setFont(incomeFont);
            incomeStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

            CellStyle expenseStyle = wb.createCellStyle();
            Font expenseFont = wb.createFont();
            expenseFont.setColor(IndexedColors.RED.getIndex());
            expenseFont.setBold(true);
            expenseStyle.setFont(expenseFont);
            expenseStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

            CellStyle totalStyle = wb.createCellStyle();
            Font totalFont = wb.createFont();
            totalFont.setBold(true);
            totalFont.setFontHeightInPoints((short) 11);
            totalStyle.setFont(totalFont);
            totalStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

            // ── Title ───────────────────────────────
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("HELMA — Transactions Export");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

            Row infoRow = sheet.createRow(1);
            infoRow.createCell(0).setCellValue("User: " + user.getFullName() + " (" + user.getEmail() + ")");
            Row infoRow2 = sheet.createRow(2);
            infoRow2.createCell(0).setCellValue("Period: " + monthStart.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

            // ── Headers ─────────────────────────────
            int headerRowIdx = 4;
            Row hRow = sheet.createRow(headerRowIdx);
            String[] headers = {"Date", "Type", "Category", "Amount (TND)", "Receipt"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = hRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // ── Data rows ───────────────────────────
            double totalIncome = 0;
            double totalExpense = 0;
            int rowIdx = headerRowIdx + 1;

            for (Transaction tx : txs) {
                Row row = sheet.createRow(rowIdx++);

                // Date
                Cell dateCell = row.createCell(0);
                dateCell.setCellValue(java.util.Date.from(tx.getTxnDate()));
                dateCell.setCellStyle(dateStyle);

                // Type
                row.createCell(1).setCellValue(tx.getType().name());

                // Category
                row.createCell(2).setCellValue(tx.getCategory() != null ? tx.getCategory() : "—");

                // Amount
                Cell amountCell = row.createCell(3);
                double amt = tx.getAmount() != null ? tx.getAmount().doubleValue() : 0;
                amountCell.setCellValue(amt);
                amountCell.setCellStyle(tx.getType() == Transaction.TransactionType.INCOME ? incomeStyle : expenseStyle);

                if (tx.getType() == Transaction.TransactionType.INCOME) totalIncome += amt;
                else totalExpense += amt;

                // Receipt
                row.createCell(4).setCellValue(tx.getReceiptUrl() != null ? "Yes" : "—");
            }

            // ── Summary rows ────────────────────────
            rowIdx++;
            Row sumRow1 = sheet.createRow(rowIdx++);
            sumRow1.createCell(2).setCellValue("Total Income");
            Cell incCell = sumRow1.createCell(3);
            incCell.setCellValue(totalIncome);
            incCell.setCellStyle(totalStyle);

            Row sumRow2 = sheet.createRow(rowIdx++);
            sumRow2.createCell(2).setCellValue("Total Expense");
            Cell expCell = sumRow2.createCell(3);
            expCell.setCellValue(totalExpense);
            expCell.setCellStyle(totalStyle);

            Row sumRow3 = sheet.createRow(rowIdx);
            sumRow3.createCell(2).setCellValue("Net Flow");
            Cell netCell = sumRow3.createCell(3);
            netCell.setCellValue(totalIncome - totalExpense);
            netCell.setCellStyle(totalStyle);

            // ── Auto-size columns ───────────────────
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // ── Write to bytes ──────────────────────
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate Excel", e);
        }
    }
}