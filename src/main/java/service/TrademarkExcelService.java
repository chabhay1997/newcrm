package service;

import model.User;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TrademarkExcelService {
    public record ImportResult(int imported, int duplicates, int invalid, List<String> errors) { }
    private static final String[] HEADERS = {"Company Name", "Date", "Contact Number", "Email ID",
            "Trade Mark Class", "Trade Mark Name", "Application Number", "Payment Status", "Mark Status",
            "Status", "Validity", "Address", "Remark"};
    private final JdbcTemplate jdbc;

    public TrademarkExcelService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public ImportResult importFile(User user, MultipartFile file) {
        validateFile(file);
        int imported = 0, duplicates = 0, invalid = 0;
        List<String> errors = new ArrayList<>();
        Set<String> knownKeys = existingKeys();
        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String,Integer> headers = headers(sheet.getRow(sheet.getFirstRowNum()));
            require(headers, "company name", "company");
            require(headers, "date", "application date");
            for (int index = sheet.getFirstRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null || blank(row)) continue;
                try {
                    String company = text(row, headers, "company name", "company");
                    LocalDate date = date(row, headers, "date", "application date");
                    if (company.isBlank()) throw new IllegalArgumentException("Company Name is required");
                    if (date == null) throw new IllegalArgumentException("Date is required");
                    String contact = text(row, headers, "contact number", "contact no", "phone", "mobile");
                    String email = text(row, headers, "email id", "email", "mail id", "mail");
                    String tradeClass = text(row, headers, "trade mark class", "trademark class", "class");
                    String tradeName = text(row, headers, "trade mark name", "trademark name", "mark name");
                    String appNo = text(row, headers, "application number", "application no", "app no");
                    Integer payment = payment(text(row, headers, "payment status", "payment"));
                    String markStatus = text(row, headers, "mark status", "mark type");
                    String status = text(row, headers, "status", "project status");
                    String validity = text(row, headers, "validity");
                    String address = text(row, headers, "address");
                    String remark = text(row, headers, "remark", "remarks");
                    String key = duplicateKey(appNo, company, contact, tradeClass, tradeName, date);
                    if (!knownKeys.add(key)) { duplicates++; continue; }
                    jdbc.update("""
                            insert into trademarks
                            (created_by, company_name, contact_no, mail_id, trade_mark_class, trade_mark_name,
                             app_no, payment_status, mark_status, status, validity, address, remark, date, created_at, updated_at)
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                            """, user.getId(), nullable(company), nullable(contact), nullable(email), nullable(tradeClass),
                            nullable(tradeName), nullable(appNo), payment, nullable(markStatus), nullable(status),
                            nullable(validity), nullable(address), nullable(remark), date);
                    imported++;
                } catch (Exception exception) {
                    invalid++;
                    errors.add("Row " + (index + 1) + ": " + exception.getMessage());
                }
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to read Excel file: " + exception.getMessage());
        }
        return new ImportResult(imported, duplicates, invalid, errors.stream().limit(10).toList());
    }

    public byte[] export(List<Map<String,Object>> records) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Trademarks"); sheet.createFreezePane(0, 1);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex()); headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont(); headerFont.setBold(true); headerFont.setColor(IndexedColors.WHITE.getIndex()); headerStyle.setFont(headerFont);
            Row header = sheet.createRow(0);
            for (int column = 0; column < HEADERS.length; column++) { Cell cell = header.createCell(column); cell.setCellValue(HEADERS[column]); cell.setCellStyle(headerStyle); }
            for (int index = 0; index < records.size(); index++) {
                Map<String,Object> record = records.get(index); Row row = sheet.createRow(index + 1);
                Object[] values = {record.get("company_name"), record.get("date"), record.get("contact_no"), record.get("mail_id"),
                        record.get("trade_mark_class"), record.get("trade_mark_name"), record.get("app_no"), paymentText(record.get("payment_status")),
                        record.get("mark_status"), record.get("status"), record.get("validity"), record.get("address"), record.get("remark")};
                for (int column = 0; column < values.length; column++) row.createCell(column).setCellValue(safe(values[column]));
            }
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, records.size()), 0, HEADERS.length - 1));
            for (int column = 0; column < HEADERS.length; column++) { sheet.autoSizeColumn(column); sheet.setColumnWidth(column, Math.min(sheet.getColumnWidth(column) + 512, 18000)); }
            workbook.write(output); return output.toByteArray();
        } catch (Exception exception) { throw new IllegalStateException("Unable to export trademark records", exception); }
    }

    private Set<String> existingKeys() {
        Set<String> keys = new HashSet<>();
        jdbc.queryForList("select app_no, company_name, contact_no, trade_mark_class, trade_mark_name, date from trademarks")
                .forEach(row -> keys.add(duplicateKey(value(row,"app_no"), value(row,"company_name"), value(row,"contact_no"),
                        value(row,"trade_mark_class"), value(row,"trade_mark_name"), localDate(row.get("date")))));
        return keys;
    }
    private String duplicateKey(String appNo, String company, String contact, String tradeClass, String tradeName, LocalDate date) {
        if (!normalize(appNo).isBlank()) return "app|" + normalize(appNo);
        return String.join("|", "record", normalize(company), normalize(contact), normalize(tradeClass), normalize(tradeName), date == null ? "" : date.toString());
    }
    private void validateFile(MultipartFile file) { if (file == null || file.isEmpty()) throw new IllegalArgumentException("Choose an Excel file to import"); String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT); if (!name.endsWith(".xlsx") && !name.endsWith(".xls")) throw new IllegalArgumentException("Only .xlsx and .xls files can be imported"); }
    private Map<String,Integer> headers(Row row) { if (row == null) throw new IllegalArgumentException("Excel header row is missing"); Map<String,Integer> map = new HashMap<>(); DataFormatter formatter = new DataFormatter(); for (Cell cell : row) map.put(normalizeHeader(formatter.formatCellValue(cell)), cell.getColumnIndex()); return map; }
    private void require(Map<String,Integer> headers, String... names) { for (String name : names) if (headers.containsKey(normalizeHeader(name))) return; throw new IllegalArgumentException("Missing required Excel column: " + names[0]); }
    private String text(Row row, Map<String,Integer> headers, String... names) { for (String name : names) { Integer column = headers.get(normalizeHeader(name)); if (column != null && row.getCell(column) != null) return new DataFormatter().formatCellValue(row.getCell(column)).trim(); } return ""; }
    private LocalDate date(Row row, Map<String,Integer> headers, String... names) { for (String name : names) { Integer column = headers.get(normalizeHeader(name)); if (column == null) continue; Cell cell = row.getCell(column); if (cell == null) return null; if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(); String value = new DataFormatter().formatCellValue(cell).trim(); if (value.isBlank()) return null; for (DateTimeFormatter format : List.of(DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("dd/MM/uuuu"), DateTimeFormatter.ofPattern("dd-MM-uuuu"), DateTimeFormatter.ofPattern("dd MMM uuuu", Locale.ENGLISH))) try { return LocalDate.parse(value, format); } catch (Exception ignored) {} throw new IllegalArgumentException("Date must use YYYY-MM-DD or DD/MM/YYYY"); } return null; }
    private Integer payment(String value) { if (value == null || value.isBlank()) return null; String normalized = normalize(value); if (Set.of("1","yes","paid","true").contains(normalized)) return 1; if (Set.of("2","half","partial","partially paid").contains(normalized)) return 2; if (Set.of("0","no","unpaid","false").contains(normalized)) return 0; throw new IllegalArgumentException("Payment Status must be Yes, Half, No, 1, 2 or 0"); }
    private boolean blank(Row row) { DataFormatter formatter = new DataFormatter(); for (Cell cell : row) if (!formatter.formatCellValue(cell).trim().isBlank()) return false; return true; }
    private String normalizeHeader(String value) { return normalize(value).replaceAll("[._]", "").replaceAll("\\s+", " "); }
    private String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private String nullable(String value) { String clean = value == null ? "" : value.trim(); return clean.isEmpty() ? null : clean; }
    private String safe(Object value) { return value == null ? "" : value.toString().replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", " "); }
    private String value(Map<String,Object> row, String key) { return row.get(key) == null ? "" : row.get(key).toString(); }
    private LocalDate localDate(Object value) { if (value == null) return null; if (value instanceof java.sql.Date date) return date.toLocalDate(); if (value instanceof LocalDate date) return date; try { return LocalDate.parse(value.toString().substring(0,10)); } catch (Exception ignored) { return null; } }
    private String paymentText(Object value) { if (value == null) return ""; return "1".equals(value.toString()) ? "Yes" : "2".equals(value.toString()) ? "Half" : "0".equals(value.toString()) ? "No" : value.toString(); }
}
