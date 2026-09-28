package service;

import model.User;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.util.*;

@Service
public class FmcsOneExcelService {
    public record ImportResult(int imported, int duplicates, int invalid, List<String> errors) {}
    private static final String[] HEADERS = {"CML No", "Company Name", "Client Name", "Client Email", "Indian Standard", "Product Name", "Application No", "BIS Payment", "License Status"};
    private final JdbcTemplate jdbc;

    public FmcsOneExcelService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public byte[] export(List<Map<String, Object>> records) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("FMCS List"); sheet.createFreezePane(0, 1);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont(); font.setBold(true); font.setColor(IndexedColors.WHITE.getIndex()); headerStyle.setFont(font);
            Row header = sheet.createRow(0);
            for (int column = 0; column < HEADERS.length; column++) { Cell cell = header.createCell(column); cell.setCellValue(HEADERS[column]); cell.setCellStyle(headerStyle); }
            for (int index = 0; index < records.size(); index++) {
                Map<String, Object> record = records.get(index); Row row = sheet.createRow(index + 1);
                Object[] values = {record.get("cml_no"), record.get("company_name"), record.get("client_name"), record.get("client_email"), record.get("indian_standard"), record.get("product_name"), record.get("app_no"), record.get("bis_pay"), record.get("license_status")};
                for (int column = 0; column < values.length; column++) row.createCell(column).setCellValue(safe(values[column]));
            }
            for (int column = 0; column < HEADERS.length; column++) { sheet.autoSizeColumn(column); sheet.setColumnWidth(column, Math.min(sheet.getColumnWidth(column) + 600, 16000)); }
            workbook.write(output); return output.toByteArray();
        } catch (Exception exception) { throw new IllegalStateException("Unable to export FMCS records", exception); }
    }

    @Transactional
    public ImportResult importFile(User user, MultipartFile file) {
        validate(file); Set<String> columns = tableColumns(); Set<String> known = knownKeys();
        int imported = 0, duplicates = 0, invalid = 0; List<String> errors = new ArrayList<>();
        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheetAt(0); Map<String, Integer> headers = headers(sheet.getRow(sheet.getFirstRowNum()));
            for (int index = sheet.getFirstRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index); if (row == null || blank(row)) continue;
                try {
                    Map<String, Object> candidate = new LinkedHashMap<>();
                    put(candidate, columns, "cml_no", text(row, headers, "cml no", "cml number"));
                    put(candidate, columns, "company_name", text(row, headers, "company name", "company"));
                    put(candidate, columns, "client_name", text(row, headers, "client name", "client"));
                    put(candidate, columns, "client_email", text(row, headers, "client email", "email", "email id"));
                    put(candidate, columns, "indian_standard", text(row, headers, "indian standard", "is info", "is number"));
                    put(candidate, columns, "product_name", text(row, headers, "product name", "product"));
                    put(candidate, columns, "app_no", text(row, headers, "application no", "app no", "application number"));
                    put(candidate, columns, "bis_pay", text(row, headers, "bis payment", "payment", "payment status"));
                    put(candidate, columns, "license_status", text(row, headers, "license status", "status"));
                    if (key(candidate).equals("|||")) throw new IllegalArgumentException("Provide an application number, CML number, company name, or client email");
                    if (columns.contains("created_by")) candidate.put("created_by", user.getId());
                    if (columns.contains("updated_by")) candidate.put("updated_by", user.getId());
                    if (columns.contains("created_at")) candidate.put("created_at", new Timestamp(System.currentTimeMillis()));
                    if (columns.contains("updated_at")) candidate.put("updated_at", new Timestamp(System.currentTimeMillis()));
                    if (!known.add(key(candidate))) { duplicates++; continue; }
                    String names = String.join(",", candidate.keySet()); String marks = String.join(",", Collections.nCopies(candidate.size(), "?"));
                    jdbc.update("insert into f_m_c_s_operations (" + names + ") values (" + marks + ")", candidate.values().toArray()); imported++;
                } catch (Exception exception) { invalid++; if (errors.size() < 10) errors.add("Row " + (index + 1) + ": " + exception.getMessage()); }
            }
        } catch (IllegalArgumentException exception) { throw exception; }
        catch (Exception exception) { throw new IllegalArgumentException("Unable to read Excel file: " + exception.getMessage()); }
        return new ImportResult(imported, duplicates, invalid, errors);
    }

    private Set<String> knownKeys() { Set<String> keys = new HashSet<>(); jdbc.queryForList("select cml_no, app_no, company_name, client_email from f_m_c_s_operations").forEach(row -> keys.add(key(row))); return keys; }
    private Set<String> tableColumns() { return jdbc.query("select * from f_m_c_s_operations limit 0", rs -> { Set<String> result = new HashSet<>(); ResultSetMetaData meta = rs.getMetaData(); for (int index = 1; index <= meta.getColumnCount(); index++) result.add(meta.getColumnLabel(index).toLowerCase(Locale.ROOT)); return result; }); }
    private void put(Map<String, Object> candidate, Set<String> columns, String column, String value) { if (columns.contains(column) && !value.isBlank()) candidate.put(column, value); }
    private String key(Map<String, Object> row) { return normalized(row.get("app_no")) + "|" + normalized(row.get("cml_no")) + "|" + normalized(row.get("company_name")) + "|" + normalized(row.get("client_email")); }
    private String normalized(Object value) { return value == null ? "" : value.toString().trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " "); }
    private Map<String, Integer> headers(Row row) { if (row == null) throw new IllegalArgumentException("Excel header row is missing."); Map<String, Integer> result = new HashMap<>(); DataFormatter formatter = new DataFormatter(); for (Cell cell : row) result.put(normalize(formatter.formatCellValue(cell)), cell.getColumnIndex()); return result; }
    private String text(Row row, Map<String, Integer> headers, String... names) { for (String name : names) { Integer index = headers.get(normalize(name)); if (index != null && row.getCell(index) != null) return new DataFormatter().formatCellValue(row.getCell(index)).trim(); } return ""; }
    private boolean blank(Row row) { DataFormatter formatter = new DataFormatter(); for (Cell cell : row) if (!formatter.formatCellValue(cell).trim().isBlank()) return false; return true; }
    private String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("[._]", "").replaceAll("\\s+", " "); }
    private String safe(Object value) { return value == null ? "" : value.toString(); }
    private void validate(MultipartFile file) { if (file == null || file.isEmpty()) throw new IllegalArgumentException("Choose an Excel file to import."); String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT); if (!name.endsWith(".xlsx") && !name.endsWith(".xls")) throw new IllegalArgumentException("Only .xlsx and .xls files can be imported."); }
}
