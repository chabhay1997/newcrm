package service;

import enums.InvoiceStatus;
import enums.InvoiceType;
import model.Invoice;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import repository.InvoiceRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class InvoiceExcelService {
    private static final List<String> HEADERS = List.of(
            "Invoice No.", "Customer", "Invoice Date", "PO No.", "Invoice Heading", "Currency", "Status",
            "Address", "PAN No.", "SAC Code", "GSTIN", "Particular Titles", "Particular Descriptions",
            "Line Amounts", "Net Amount", "Extra Services", "Extra Services Amount", "Final Amount",
            "Remarks", "Bank ID", "Show Signature", "Show Terms"
    );

    private final InvoiceRepository invoiceRepository;

    public InvoiceExcelService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public byte[] exportInvoices(InvoiceType type) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Invoices");
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("dd-mmm-yyyy"));

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS.get(i));
                cell.setCellStyle(headerStyle);
            }

            int rowNumber = 1;
            for (Invoice invoice : invoiceRepository.findAllByInvTypeOrderByIdDesc(type.getId())) {
                Row row = sheet.createRow(rowNumber++);
                write(row, 0, invoice.getInvNo()); write(row, 1, invoice.getName());
                if (invoice.getDate() != null) {
                    Cell dateCell = row.createCell(2);
                    dateCell.setCellValue(invoice.getDate());
                    dateCell.setCellStyle(dateStyle);
                }
                write(row, 3, invoice.getPoNo());
                write(row, 4, Objects.equals(invoice.getInvTypes(), 2) ? "Tax Invoice" : "Export Invoice");
                write(row, 5, Objects.equals(invoice.getIsInr(), 1) ? "INR" : "USD");
                write(row, 6, InvoiceStatus.fromId(invoice.getStatus()).getLabel());
                write(row, 7, invoice.getAddress()); write(row, 8, invoice.getPanNo()); write(row, 9, invoice.getSacCode());
                write(row, 10, invoice.getGstin()); write(row, 11, invoice.getTitle()); write(row, 12, invoice.getParticular());
                write(row, 13, invoice.getAmtD()); write(row, 14, invoice.getNetAmt()); write(row, 15, invoice.getExtraServices());
                write(row, 16, invoice.getExtraAmt()); write(row, 17, invoice.getFinalAmt()); write(row, 18, invoice.getRemarks());
                if (invoice.getBankType() != null) row.createCell(19).setCellValue(invoice.getBankType());
                write(row, 20, Objects.equals(invoice.getIsSign(), 1L) ? "No" : "Yes");
                write(row, 21, Objects.equals(invoice.getIsTerms(), 1) ? "Yes" : "No");
            }
            for (int i = 0; i < HEADERS.size(); i++) sheet.setColumnWidth(i, Math.min(50, Math.max(14, HEADERS.get(i).length() + 3)) * 256);
            sheet.createFreezePane(0, 1);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    @Transactional
    public ImportResult importInvoices(InvoiceType type, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Please choose a non-empty Excel file.");
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".xlsx")) throw new IllegalArgumentException("Only .xlsx Excel files are supported.");

        int imported = 0, skipped = 0;
        DataFormatter formatter = new DataFormatter();
        // Build this once so a re-upload and repeated rows in the same workbook are both idempotent.
        Set<String> knownInvoiceNumbers = new HashSet<>();
        for (Invoice existing : invoiceRepository.findAllByInvTypeOrderByIdDesc(type.getId())) {
            String invoiceNumber = normalizeInvoiceNumber(existing.getInvNo());
            if (!invoiceNumber.isEmpty()) knownInvoiceNumbers.add(invoiceNumber);
        }
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) throw new IllegalArgumentException("The Excel file has no invoice rows.");
            Map<String, Integer> columns = headerMap(sheet.getRow(0), formatter);
            require(columns, "Invoice No.", "Customer");

            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) continue;
                String invNo = value(row, columns, "Invoice No.", formatter).trim();
                String customer = value(row, columns, "Customer", formatter).trim();
                String invoiceNumber = normalizeInvoiceNumber(invNo);
                if (invNo.isEmpty() && customer.isEmpty()) continue;
                if (invoiceNumber.isEmpty() || customer.isEmpty() || knownInvoiceNumbers.contains(invoiceNumber)) {
                    skipped++;
                    continue;
                }
                try {
                    Invoice invoice = new Invoice();
                    invoice.setInvType(type.getId()); invoice.setInvNo(invNo); invoice.setName(customer);
                    invoice.setDate(date(row, columns.get("Invoice Date"), formatter));
                    invoice.setPoNo(value(row, columns, "PO No.", formatter));
                    invoice.setInvTypes(value(row, columns, "Invoice Heading", formatter).toLowerCase(Locale.ROOT).contains("tax") ? 2 : 1);
                    invoice.setIsInr(value(row, columns, "Currency", formatter).equalsIgnoreCase("INR") ? 1 : 0);
                    invoice.setStatus(status(value(row, columns, "Status", formatter)));
                    invoice.setAddress(value(row, columns, "Address", formatter)); invoice.setPanNo(value(row, columns, "PAN No.", formatter));
                    invoice.setSacCode(value(row, columns, "SAC Code", formatter)); invoice.setGstin(value(row, columns, "GSTIN", formatter));
                    invoice.setTitle(value(row, columns, "Particular Titles", formatter)); invoice.setParticular(value(row, columns, "Particular Descriptions", formatter));
                    invoice.setAmtD(value(row, columns, "Line Amounts", formatter)); invoice.setNetAmt(amount(value(row, columns, "Net Amount", formatter)));
                    invoice.setExtraServices(value(row, columns, "Extra Services", formatter)); invoice.setExtraAmt(amount(value(row, columns, "Extra Services Amount", formatter)));
                    invoice.setFinalAmt(amount(value(row, columns, "Final Amount", formatter))); invoice.setRemarks(value(row, columns, "Remarks", formatter));
                    invoice.setBankType(longValue(value(row, columns, "Bank ID", formatter)));
                    invoice.setIsSign(yes(value(row, columns, "Show Signature", formatter), true) ? 0L : 1L);
                    invoice.setIsTerms(yes(value(row, columns, "Show Terms", formatter), false) ? 1 : 0);
                    invoice.setCreatedBy(util.AuthUtil.currentUserId()); invoice.setCreatedAt(LocalDateTime.now()); invoice.setUpdatedAt(LocalDateTime.now());
                    if (invoice.getDate() == null) invoice.setDate(LocalDate.now());
                    if (invoice.getFinalAmt().isBlank()) invoice.setFinalAmt(invoice.getNetAmt());
                    invoiceRepository.save(invoice);
                    knownInvoiceNumbers.add(invoiceNumber);
                    imported++;
                } catch (RuntimeException ex) {
                    skipped++;
                }
            }
        }
        return new ImportResult(imported, skipped);
    }

    private Map<String, Integer> headerMap(Row row, DataFormatter formatter) {
        if (row == null) throw new IllegalArgumentException("The Excel header row is missing.");
        Map<String, Integer> result = new HashMap<>();
        for (Cell cell : row) result.put(formatter.formatCellValue(cell).trim(), cell.getColumnIndex());
        return result;
    }
    private void require(Map<String, Integer> columns, String... names) {
        for (String name : names) if (!columns.containsKey(name)) throw new IllegalArgumentException("Missing required Excel column: " + name);
    }
    private String value(Row row, Map<String, Integer> columns, String name, DataFormatter formatter) {
        Integer column = columns.get(name); return column == null ? "" : formatter.formatCellValue(row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)).trim();
    }
    private LocalDate date(Row row, Integer column, DataFormatter formatter) {
        if (column == null) return null;
        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        String raw = formatter.formatCellValue(cell).trim();
        if (raw.isEmpty()) return null;
        for (java.time.format.DateTimeFormatter format : List.of(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"), java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"))) {
            try { return LocalDate.parse(raw, format); } catch (Exception ignored) { }
        }
        throw new IllegalArgumentException("Invalid date");
    }
    private int status(String value) {
        if (value.equalsIgnoreCase("Paid") || value.equals("1")) return 1;
        if (value.equalsIgnoreCase("Half") || value.equals("2")) return 2;
        return 3;
    }
    private boolean yes(String value, boolean defaultValue) { return value.isBlank() ? defaultValue : value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true") || value.equals("1"); }
    private Long longValue(String value) { try { return value.isBlank() ? null : new BigDecimal(value).longValueExact(); } catch (Exception ignored) { return null; } }
    private String amount(String value) { try { return value.isBlank() ? "" : new BigDecimal(value.replace(",", "")).stripTrailingZeros().toPlainString(); } catch (Exception ignored) { return ""; } }
    private String normalizeInvoiceNumber(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
    private void write(Row row, int column, String value) { row.createCell(column).setCellValue(value == null ? "" : value); }

    public record ImportResult(int imported, int skipped) { }
}
