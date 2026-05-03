package com.helma.helmabackend.service.crowdfunding;

import com.helma.helmabackend.dto.crowdfunding.PortfolioImportResultResponse;
import com.helma.helmabackend.dto.crowdfunding.PortfolioImportRowResponse;
import com.helma.helmabackend.dto.crowdfunding.PortfolioPositionResponse;
import com.helma.helmabackend.entity.crowdfunding.PortfolioImportedPosition;
import com.helma.helmabackend.entity.crowdfunding.enums.AppTag;
import com.helma.helmabackend.entity.crowdfunding.enums.Sector;
import com.helma.helmabackend.entity.crowdfunding.enums.SubSector;
import com.helma.helmabackend.entity.user.Role;
import com.helma.helmabackend.entity.user.User;
import com.helma.helmabackend.exception.UnauthorizedException;
import com.helma.helmabackend.repository.crowdfunding.PortfolioImportedPositionRepository;
import com.helma.helmabackend.service.user.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PortfolioPositionXlsxService {

    private static final String[] HEADERS = {
            "Campaign Name",
            "Source",
            "Sector",
            "Sub Sector",
            "Governorate",
            "City",
            "Tags",
            "Invested Amount",
            "Currency",
            "Portfolio Weight %",
            "Funding Goal",
            "Raised Amount",
            "Funding Progress %",
            "Equity Offered %",
            "Ownership %",
            "Investment Date",
            "Source Reference",
            "Notes"
    };

    private final PortfolioImportedPositionRepository importedPositionRepository;
    private final CurrentUserService currentUserService;

    public byte[] exportPositions(List<PortfolioPositionResponse> positions) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Portfolio Positions");
            CellStyle headerStyle = buildHeaderStyle(workbook);
            CellStyle moneyStyle = buildNumberStyle(workbook, "#,##0.000");
            CellStyle percentStyle = buildNumberStyle(workbook, "0.000");
            CellStyle dateStyle = buildDateStyle(workbook);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (PortfolioPositionResponse position : positions) {
                Row row = sheet.createRow(rowIndex++);
                writeString(row, 0, position.campaignBusinessName);
                writeString(row, 1, position.positionSource == null ? "HELMA" : position.positionSource);
                writeString(row, 2, enumName(position.sector));
                writeString(row, 3, enumName(position.subSector));
                writeString(row, 4, position.governorate);
                writeString(row, 5, position.city);
                writeString(row, 6, joinTags(position.tags));
                writeNumber(row, 7, position.investedAmount, moneyStyle);
                writeString(row, 8, position.currency);
                writeNumber(row, 9, position.positionWeightPct, percentStyle);
                writeNumber(row, 10, position.campaignFundingGoal, moneyStyle);
                writeNumber(row, 11, position.campaignRaisedAmount, moneyStyle);
                writeNumber(row, 12, position.campaignFundingProgressPct, percentStyle);
                writeNumber(row, 13, position.equityOfferedPercent, percentStyle);
                writeNumber(row, 14, position.ownershipPercent, percentStyle);
                writeDate(row, 15, position.pledgedAt, dateStyle);
                writeString(row, 16, position.sourceReference);
                writeString(row, 17, position.notes);
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not export portfolio positions.", ex);
        }
    }

    public byte[] buildImportTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Import Positions");
            CellStyle headerStyle = buildHeaderStyle(workbook);
            CellStyle moneyStyle = buildNumberStyle(workbook, "#,##0.000");
            CellStyle percentStyle = buildNumberStyle(workbook, "0.000");
            CellStyle dateStyle = buildDateStyle(workbook);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            Row sample = sheet.createRow(1);
            writeString(sample, 0, "Example Equity Campaign");
            writeString(sample, 1, "IMPORTED_XLSX");
            writeString(sample, 2, "TECHNOLOGY");
            writeString(sample, 3, "SAAS");
            writeString(sample, 4, "Tunis");
            writeString(sample, 5, "Tunis");
            writeString(sample, 6, "B2B, SOFTWARE");
            writeNumber(sample, 7, new BigDecimal("1000.000"), moneyStyle);
            writeString(sample, 8, "TND");
            writeNumber(sample, 9, null, percentStyle);
            writeNumber(sample, 10, new BigDecimal("50000.000"), moneyStyle);
            writeNumber(sample, 11, new BigDecimal("25000.000"), moneyStyle);
            writeNumber(sample, 12, null, percentStyle);
            writeNumber(sample, 13, new BigDecimal("10.000"), percentStyle);
            writeNumber(sample, 14, null, percentStyle);
            writeDate(sample, 15, Instant.now(), dateStyle);
            writeString(sample, 16, "BANK-REF-001");
            writeString(sample, 17, "Imported external tracked equity position");

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not build portfolio import template.", ex);
        }
    }

    @Transactional
    public PortfolioImportResultResponse importPositions(MultipartFile file, boolean replaceExisting) {
        User me = currentUserService.getCurrentUser();
        requireInvestor(me);

        PortfolioImportResultResponse result = parseWorkbook(file);
        result.replaceExisting = replaceExisting;

        if (result.invalidRows > 0) {
            result.message = "Import stopped. Fix invalid rows and upload the XLSX again.";
            return result;
        }

        List<PortfolioImportedPosition> positions = result.rows.stream()
                .filter(row -> Boolean.TRUE.equals(row.valid))
                .map(row -> toEntity(row, me.getId()))
                .toList();

        if (replaceExisting) {
            importedPositionRepository.deleteByInvestorUserId(me.getId());
        }

        importedPositionRepository.saveAll(positions);
        result.importedRows = positions.size();
        result.message = positions.isEmpty()
                ? "No rows were imported."
                : positions.size() + " imported tracked position" + (positions.size() == 1 ? "" : "s") + " saved.";

        return result;
    }

    private PortfolioImportResultResponse parseWorkbook(MultipartFile file) {
        PortfolioImportResultResponse result = new PortfolioImportResultResponse();

        if (file == null || file.isEmpty()) {
            result.message = "Upload a non-empty XLSX file.";
            return result;
        }

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() <= 1) {
                result.message = "The XLSX file does not contain position rows.";
                return result;
            }

            DataFormatter formatter = new DataFormatter(Locale.US);
            Map<String, Integer> columns = readHeaderColumns(sheet.getRow(0), formatter);

            int lastRow = sheet.getLastRowNum();
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row, formatter)) {
                    continue;
                }

                PortfolioImportRowResponse parsed = parseRow(row, columns, formatter);
                result.rows.add(parsed);
            }

            result.totalRows = result.rows.size();
            result.validRows = (int) result.rows.stream().filter(r -> Boolean.TRUE.equals(r.valid)).count();
            result.invalidRows = result.totalRows - result.validRows;
            return result;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not read XLSX file.", ex);
        }
    }

    private PortfolioImportRowResponse parseRow(Row row, Map<String, Integer> columns, DataFormatter formatter) {
        PortfolioImportRowResponse parsed = new PortfolioImportRowResponse();
        parsed.rowNumber = row.getRowNum() + 1;

        parsed.campaignBusinessName = readString(row, columns, formatter, "campaignname", "campaignbusinessname");
        parsed.sector = readString(row, columns, formatter, "sector");
        parsed.subSector = readString(row, columns, formatter, "subsector");
        parsed.governorate = readString(row, columns, formatter, "governorate", "region");
        parsed.city = readString(row, columns, formatter, "city");
        parsed.tags = splitTags(readString(row, columns, formatter, "tags", "tag"));
        parsed.investedAmount = readBigDecimal(row, columns, formatter, "investedamount", "amount");
        parsed.currency = readString(row, columns, formatter, "currency");
        parsed.campaignFundingGoal = readBigDecimal(row, columns, formatter, "fundinggoal", "campaignfundinggoal");
        parsed.campaignRaisedAmount = readBigDecimal(row, columns, formatter, "raisedamount", "campaignraisedamount");
        parsed.equityOfferedPercent = readBigDecimal(row, columns, formatter, "equityoffered", "equityofferedpercent", "equityofferedpct");
        parsed.ownershipPercent = readBigDecimal(row, columns, formatter, "ownership", "ownershippercent", "ownershippct");
        parsed.investedAt = readInstant(row, columns, formatter, "investmentdate", "investedat", "pledgedat");
        parsed.sourceReference = readString(row, columns, formatter, "sourcereference", "reference");
        parsed.notes = readString(row, columns, formatter, "notes", "note");

        validate(parsed);
        parsed.valid = parsed.errors.isEmpty();
        return parsed;
    }

    private void validate(PortfolioImportRowResponse row) {
        if (isBlank(row.campaignBusinessName)) {
            row.errors.add("Campaign Name is required.");
        }
        if (row.investedAmount == null || row.investedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            row.errors.add("Invested Amount must be greater than 0.");
        }
        if (isBlank(row.currency)) {
            row.currency = "TND";
        } else {
            row.currency = row.currency.trim().toUpperCase();
        }
        if (isBlank(row.sector)) {
            row.errors.add("Sector is required for optimisation analytics.");
        } else if (parseEnum(Sector.class, row.sector) == null) {
            row.errors.add("Sector must match one of the backend Sector enum values, for example TECHNOLOGY or FINTECH_FINANCIAL_SERVICES.");
        }
        if (!isBlank(row.subSector) && parseEnum(SubSector.class, row.subSector) == null) {
            row.errors.add("Sub Sector must match one of the backend SubSector enum values, for example SAAS or CLEAN_ENERGY.");
        }
        if (isBlank(row.governorate)) {
            row.errors.add("Governorate is required for region allocation analytics.");
        }
        for (String tag : row.tags) {
            if (!isBlank(tag) && parseEnum(AppTag.class, tag) == null) {
                row.errors.add("Unknown tag: " + tag + ". Use values like B2B, SOFTWARE, IMPACT, SUSTAINABLE.");
            }
        }
    }

    private PortfolioImportedPosition toEntity(PortfolioImportRowResponse row, Long investorUserId) {
        PortfolioImportedPosition entity = new PortfolioImportedPosition();
        entity.setInvestorUserId(investorUserId);
        entity.setCampaignBusinessName(row.campaignBusinessName);
        entity.setSector(parseEnum(Sector.class, row.sector));
        entity.setSubSector(parseEnum(SubSector.class, row.subSector));
        entity.setGovernorate(row.governorate);
        entity.setCity(row.city);
        entity.setTags(parseTags(row.tags));
        entity.setInvestedAmount(scaleMoney(row.investedAmount));
        entity.setCurrency(row.currency == null ? "TND" : row.currency.trim().toUpperCase());
        entity.setCampaignFundingGoal(scaleMoney(row.campaignFundingGoal));
        entity.setCampaignRaisedAmount(scaleMoney(row.campaignRaisedAmount));
        entity.setEquityOfferedPercent(scalePercent(row.equityOfferedPercent));
        entity.setOwnershipPercent(resolveOwnership(row));
        entity.setInvestedAt(row.investedAt == null ? Instant.now() : row.investedAt);
        entity.setSourceReference(row.sourceReference);
        entity.setNotes(row.notes);
        return entity;
    }

    private BigDecimal resolveOwnership(PortfolioImportRowResponse row) {
        if (row.ownershipPercent != null) {
            return scalePercent(row.ownershipPercent);
        }
        BigDecimal invested = row.investedAmount == null ? BigDecimal.ZERO : row.investedAmount;
        BigDecimal goal = row.campaignFundingGoal == null ? BigDecimal.ZERO : row.campaignFundingGoal;
        BigDecimal offered = row.equityOfferedPercent == null ? BigDecimal.ZERO : row.equityOfferedPercent;
        if (invested.signum() <= 0 || goal.signum() <= 0 || offered.signum() <= 0) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        return invested.divide(goal, 8, RoundingMode.HALF_UP)
                .multiply(offered)
                .setScale(6, RoundingMode.HALF_UP);
    }

    private CellStyle buildHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle buildNumberStyle(Workbook workbook, String format) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat(format));
        return style;
    }

    private CellStyle buildDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper helper = workbook.getCreationHelper();
        style.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd"));
        return style;
    }

    private void writeString(Row row, int column, String value) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
    }

    private void writeNumber(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private void writeDate(Row row, int column, Instant instant, CellStyle style) {
        Cell cell = row.createCell(column);
        if (instant != null) {
            cell.setCellValue(java.util.Date.from(instant));
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private String enumName(Object value) {
        return value == null ? "" : value.toString();
    }

    private String joinTags(Set<AppTag> tags) {
        if (tags == null || tags.isEmpty()) return "";
        return tags.stream().filter(Objects::nonNull).map(Enum::name).reduce((a, b) -> a + ", " + b).orElse("");
    }

    private Map<String, Integer> readHeaderColumns(Row header, DataFormatter formatter) {
        Map<String, Integer> columns = new HashMap<>();
        if (header == null) return columns;
        for (Cell cell : header) {
            String key = normalizeHeader(formatter.formatCellValue(cell));
            if (!key.isBlank()) {
                columns.put(key, cell.getColumnIndex());
            }
        }
        return columns;
    }

    private String readString(Row row, Map<String, Integer> columns, DataFormatter formatter, String... keys) {
        Cell cell = findCell(row, columns, keys);
        if (cell == null) return null;
        String value = formatter.formatCellValue(cell);
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal readBigDecimal(Row row, Map<String, Integer> columns, DataFormatter formatter, String... keys) {
        Cell cell = findCell(row, columns, keys);
        if (cell == null) return null;
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING:
                case FORMULA:
                default:
                    String raw = formatter.formatCellValue(cell);
                    if (raw == null || raw.trim().isEmpty()) return null;
                    String normalized = raw.trim().replace("%", "").replace(",", "");
                    return new BigDecimal(normalized);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private Instant readInstant(Row row, Map<String, Integer> columns, DataFormatter formatter, String... keys) {
        Cell cell = findCell(row, columns, keys);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toInstant();
            }
            String raw = formatter.formatCellValue(cell);
            if (raw == null || raw.trim().isEmpty()) return null;
            String value = raw.trim();
            try {
                return Instant.parse(value);
            } catch (DateTimeParseException ignored) {
                return LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private Cell findCell(Row row, Map<String, Integer> columns, String... keys) {
        for (String key : keys) {
            Integer index = columns.get(normalizeHeader(key));
            if (index != null) {
                return row.getCell(index);
            }
        }
        return null;
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private List<String> splitTags(String raw) {
        List<String> tags = new ArrayList<>();
        if (raw == null || raw.isBlank()) return tags;
        for (String part : raw.split("[,;|]")) {
            String normalized = part.trim();
            if (!normalized.isEmpty()) {
                tags.add(normalized);
            }
        }
        return tags;
    }

    private Set<AppTag> parseTags(List<String> rawTags) {
        Set<AppTag> tags = new LinkedHashSet<>();
        if (rawTags == null) return tags;
        for (String raw : rawTags) {
            AppTag tag = parseEnum(AppTag.class, raw);
            if (tag != null) tags.add(tag);
        }
        return tags;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String raw) {
        if (raw == null || raw.isBlank()) return null;
        String normalized = normalizeEnum(raw);
        try {
            return Enum.valueOf(enumType, normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String normalizeHeader(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String normalizeEnum(String value) {
        return value.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value == null ? null : value.setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal scalePercent(BigDecimal value) {
        return value == null ? null : value.setScale(6, RoundingMode.HALF_UP);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void requireInvestor(User u) {
        if (u.getRole() != Role.INVESTOR) {
            throw new UnauthorizedException("Only INVESTOR can perform this action.");
        }
    }
}
