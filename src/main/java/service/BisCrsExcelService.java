package service;

import model.BisCrsRenewal;
import model.User;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import repository.BisCrsRenewalRepository;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

@Service
public class BisCrsExcelService {
    public record ImportResult(int imported, int skipped, List<String> errors) { }

    private static final String[] COLUMNS = {"License No.", "Manufacturer Name", "Product Name", "Air Name", "Brand Name", "Date", "Status"};
    private final BisCrsRenewalRepository renewals;

    public BisCrsExcelService(BisCrsRenewalRepository renewals) {
        this.renewals = renewals;
    }

    public ImportResult importFile(User user, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Choose an Excel file to import");
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))
            throw new IllegalArgumentException("Only .xlsx and .xls files can be imported");

        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        Set<DuplicateKey> workbookRecords = new HashSet<>();
        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headers = headers(sheet.getRow(sheet.getFirstRowNum()));
            requireHeader(headers, "license no", "license number", "lic no");
            requireHeader(headers, "manufacturer name");
            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || blank(row)) continue;
                try {
                    String licenceNumber = text(row, headers, "license no", "license no.", "license number", "lic no");
                    String manufacturerName = text(row, headers, "manufacturer name");
                    if (licenceNumber.isBlank()) throw new IllegalArgumentException("License No. is required");
                    if (manufacturerName.isBlank()) throw new IllegalArgumentException("Manufacturer Name is required");

                    String productName = text(row, headers, "product name");
                    String airName = text(row, headers, "air name");
                    String brandName = text(row, headers, "brand name");
                    LocalDate licenceDate = date(row, headers, "date", "date of lic", "date of license", "license date");
                    String currentStatus = text(row, headers, "status", "current status");
                    DuplicateKey duplicateKey = new DuplicateKey(licenceNumber, manufacturerName, productName, airName,
                            brandName, licenceDate, currentStatus);
                    if (!workbookRecords.add(duplicateKey) || renewals.existsExactRecord(licenceNumber, manufacturerName,
                            productName, airName, brandName, licenceDate, currentStatus)) {
                        skipped++;
                        continue;
                    }

                    BisCrsRenewal record = new BisCrsRenewal();
                    record.setCreatedBy(user.getId());
                    record.setLicenceNumber(licenceNumber);
                    record.setManufacturerName(manufacturerName);
                    record.setProductName(productName);
                    record.setAirName(airName);
                    record.setBrandName(brandName);
                    record.setLicenceDate(licenceDate);
                    record.setCurrentStatus(currentStatus);
                    renewals.save(record);
                    imported++;
                } catch (Exception exception) {
                    skipped++;
                    errors.add("Row " + (rowIndex + 1) + ": " + exception.getMessage());
                }
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to read Excel file: " + exception.getMessage());
        }
        return new ImportResult(imported, skipped, errors.stream().limit(10).toList());
    }

    public byte[] export(List<BisCrsRenewal> records) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("BIS CRS");
            sheet.createFreezePane(0, 1);
            Row header = sheet.createRow(0);
            for (int column = 0; column < COLUMNS.length; column++) header.createCell(column).setCellValue(COLUMNS[column]);
            for (int index = 0; index < records.size(); index++) {
                BisCrsRenewal record = records.get(index);
                String[] values = {record.getLicenceNumber(), record.getManufacturerName(), record.getProductName(),
                        record.getAirName(), record.getBrandName(), record.getLicenceDate() == null ? "" : record.getLicenceDate().toString(), record.getCurrentStatus()};
                Row row = sheet.createRow(index + 1);
                for (int column = 0; column < values.length; column++) row.createCell(column).setCellValue(safe(values[column]));
            }
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, records.size()), 0, COLUMNS.length - 1));
            for (int column = 0; column < COLUMNS.length; column++) {
                sheet.autoSizeColumn(column);
                sheet.setColumnWidth(column, Math.min(sheet.getColumnWidth(column) + 512, 18000));
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to export BIS CRS records", exception);
        }
    }

    private Map<String, Integer> headers(Row row) {
        if (row == null) throw new IllegalArgumentException("Excel header row is missing");
        Map<String, Integer> headers = new HashMap<>();
        DataFormatter formatter = new DataFormatter();
        for (Cell cell : row) headers.put(normalize(formatter.formatCellValue(cell)), cell.getColumnIndex());
        return headers;
    }

    private void requireHeader(Map<String, Integer> headers, String... names) {
        for (String name : names) if (headers.containsKey(normalize(name))) return;
        throw new IllegalArgumentException("Missing required Excel column: " + names[0]);
    }

    private String text(Row row, Map<String, Integer> headers, String... names) {
        for (String name : names) {
            Integer column = headers.get(normalize(name));
            if (column != null && row.getCell(column) != null) return new DataFormatter().formatCellValue(row.getCell(column)).trim();
        }
        return "";
    }

    private LocalDate date(Row row, Map<String, Integer> headers, String... names) {
        for (String name : names) {
            Integer column = headers.get(normalize(name));
            if (column == null) continue;
            Cell cell = row.getCell(column);
            if (cell == null) return null;
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell))
                return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            String value = new DataFormatter().formatCellValue(cell).trim();
            if (value.isBlank()) return null;
            for (DateTimeFormatter format : List.of(DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("dd/MM/uuuu"), DateTimeFormatter.ofPattern("dd-MM-uuuu"), DateTimeFormatter.ofPattern("dd MMM uuuu", Locale.ENGLISH))) {
                try { return LocalDate.parse(value, format); } catch (Exception ignored) { }
            }
            throw new IllegalArgumentException("Date must use YYYY-MM-DD or DD/MM/YYYY");
        }
        return null;
    }

    private boolean blank(Row row) {
        DataFormatter formatter = new DataFormatter();
        for (Cell cell : row) if (!formatter.formatCellValue(cell).trim().isBlank()) return false;
        return true;
    }

    private String normalize(String header) { return header.trim().toLowerCase(Locale.ROOT).replaceAll("[._]", "").replaceAll("\\s+", " "); }
    private String safe(String value) { return value == null ? "" : value.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", " "); }

    private record DuplicateKey(String licenceNumber, String manufacturerName, String productName, String airName,
                                String brandName, LocalDate licenceDate, String currentStatus) {
        private DuplicateKey {
            licenceNumber = normalizedValue(licenceNumber);
            manufacturerName = normalizedValue(manufacturerName);
            productName = normalizedValue(productName);
            airName = normalizedValue(airName);
            brandName = normalizedValue(brandName);
            currentStatus = normalizedValue(currentStatus);
        }

        private static String normalizedValue(String value) {
            return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        }
    }
}
