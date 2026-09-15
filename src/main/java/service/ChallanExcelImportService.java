package service;

import dto.ChallanCreateRequest;
import model.State;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import repository.ChallanRepository;
import repository.StateRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChallanExcelImportService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private final ChallanService challanService;
    private final ChallanRepository challanRepository;
    private final StateRepository stateRepository;

    public ChallanExcelImportService(ChallanService challanService, ChallanRepository challanRepository,
                                     StateRepository stateRepository) {
        this.challanService = challanService;
        this.challanRepository = challanRepository;
        this.stateRepository = stateRepository;
    }

    public ImportResult importChallans(MultipartFile file, String username) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Please select an Excel file.");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("The Excel file must not exceed 10 MB.");
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".xlsx")) throw new IllegalArgumentException("Only .xlsx Excel files are supported.");

        DataFormatter formatter = new DataFormatter();
        Map<String, State> states = stateRepository.findAll().stream().filter(state -> state.getName() != null)
                .collect(Collectors.toMap(state -> state.getName().trim().toLowerCase(Locale.ROOT), Function.identity(), (a, b) -> a));
        int imported = 0;
        int skipped = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new IllegalArgumentException("The Excel file has no Challan rows.");
            }
            Map<String, Integer> columns = headerMap(sheet.getRow(0), formatter);
            require(columns, "Client Name", "Item Name", "Date");

            Set<String> workbookNumbers = new HashSet<>();
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) continue;
                String challanNo = value(row, columns, "Challan No.", formatter);
                String clientName = value(row, columns, "Client Name", formatter);
                String itemName = value(row, columns, "Item Name", formatter);
                if (challanNo.isBlank() && clientName.isBlank() && itemName.isBlank()) continue;
                String canonicalChallanNo = challanNo.trim().replaceFirst("(?i)^Evtl/Challan/", "Evtl/DC/");
                String normalizedNumber = canonicalChallanNo.toLowerCase(Locale.ROOT);
                if ((!normalizedNumber.isEmpty() && (!workbookNumbers.add(normalizedNumber)
                        || challanRepository.existsByChallanNoIgnoreCase(canonicalChallanNo)))
                        || clientName.isBlank() || itemName.isBlank()) {
                    skipped++;
                    continue;
                }
                try {
                    ChallanCreateRequest request = new ChallanCreateRequest();
                    request.setClientName(clientName);
                    request.setClientNumber(value(row, columns, "Client Number", formatter));
                    request.setItemName(itemName);
                    request.setBrandName(value(row, columns, "Brand Name", formatter));
                    request.setQty(value(row, columns, "Qty", formatter));
                    request.setAmount(amount(value(row, columns, "Amount", formatter), "0"));
                    request.setGst(amount(value(row, columns, "GST (%)", formatter), "0"));
                    request.setDate(date(row, columns.get("Date"), formatter));
                    request.setSampleReturnDate(date(row, columns.get("Sample Return Date"), formatter));
                    request.setAddress(value(row, columns, "Address", formatter));
                    request.setPincode(value(row, columns, "Pincode", formatter));
                    request.setRemark(value(row, columns, "Remark", formatter));
                    String stateName = value(row, columns, "State", formatter).toLowerCase(Locale.ROOT);
                    State state = states.get(stateName);
                    if (state != null) request.setStateId(state.getId());
                    challanService.createChallan(request, List.of(), username);
                    imported++;
                } catch (RuntimeException exception) {
                    skipped++;
                }
            }
        } catch (org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException exception) {
            throw new IllegalArgumentException("The selected file is not a valid .xlsx workbook.");
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
        for (String name : names) if (!columns.containsKey(name)) {
            throw new IllegalArgumentException("Missing required Excel column: " + name);
        }
    }

    private String value(Row row, Map<String, Integer> columns, String name, DataFormatter formatter) {
        Integer column = columns.get(name);
        if (column == null) return "";
        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private LocalDate date(Row row, Integer column, DataFormatter formatter) {
        if (column == null) return null;
        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        String raw = formatter.formatCellValue(cell).trim();
        if (raw.isEmpty()) return null;
        for (DateTimeFormatter format : List.of(DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd/MM/yyyy"), DateTimeFormatter.ofPattern("dd-MM-yyyy"))) {
            try { return LocalDate.parse(raw, format); } catch (RuntimeException ignored) { }
        }
        throw new IllegalArgumentException("Invalid date: " + raw);
    }

    private String amount(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        try { return new BigDecimal(value.replace(",", "").trim()).toPlainString(); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Invalid amount: " + value); }
    }

    public record ImportResult(int imported, int skipped) { }
}
