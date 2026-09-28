package service;

import model.BisIsiOperation;
import model.State;
import model.User;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import repository.StateRepository;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
public class BisIsiExcelService {
    private static final Set<String> EXCLUDED_STATES = Set.of("Vaishali", "Paschim Medinipur", "Narora", "Natwar", "Kenmore");
    public record Result(int imported, int skipped, List<String> errors) { }
    private final BisIsiService service;
    private final StateRepository states;

    public BisIsiExcelService(BisIsiService service, StateRepository states) {
        this.service = service;
        this.states = states;
    }

    public Result importFile(User user, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Choose an Excel file to import");
        int imported = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        Set<String> workbookKeys = new HashSet<>();
        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headers = headers(sheet.getRow(sheet.getFirstRowNum()));
            for (int index = sheet.getFirstRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null || blankRow(row)) continue;
                try {
                    String companyName = optional(text(row, headers, "company name", "company", "applicant name", "name of company"), "N/A");
                    String indianStandard = optional(text(row, headers, "indian standard", "is number", "is no", "is no.", "is"), "N/A");
                    String uniqueKey = companyName.trim().toLowerCase(Locale.ROOT) + "\u0000" + indianStandard.trim().toLowerCase(Locale.ROOT);
                    if (!workbookKeys.add(uniqueKey)) { skipped++; continue; }
                    if (service.operationExists(companyName, indianStandard)) { skipped++; continue; }
                    BisIsiOperation operation = new BisIsiOperation();
                    operation.setOperationDate(date(text(row, headers, "date", "operation date"), LocalDate.now()));
                    operation.setCompanyName(companyName);
                    operation.setClientName(optional(text(row, headers, "client name", "client", "applicant", "applicant name"), "N/A"));
                    operation.setIndianStandard(indianStandard);
                    operation.setContactNumber(optional(text(row, headers, "contact number", "contact no", "contact no.", "phone", "mobile", "mobile number"), "N/A"));
                    operation.setEmail(optional(text(row, headers, "client email", "email", "email id", "mail id"), "N/A"));
                    operation.setTestingStatus(allowed(text(row, headers, "testing status"), List.of("Paid","Pending"), "Pending"));
                    operation.setTestingPerson(optional(text(row, headers, "testing person", "testing incharge", "testing in-charge"), "N/A"));
                    operation.setAddress(text(row, headers, "address"));
                    String state = text(row, headers, "state");
                    if (EXCLUDED_STATES.stream().anyMatch(name -> name.equalsIgnoreCase(state)))
                        throw new IllegalArgumentException("State is not available for BIS-ISI: " + state);
                    Optional<State> matchedState = state.isBlank() ? Optional.empty() : states.findFirstByCountryIdAndNameIgnoreCase(101, state);
                    operation.setStateId(matchedState.map(State::getId).orElse(0L));
                    operation.setProjectStatus(allowed(text(row, headers, "project status", "status"), BisIsiService.STATUSES, "Registration"));
                    operation.setProcedure(allowed(text(row, headers, "procedure"), List.of("Simplified","Normal"), "Normal"));
                    operation.setPaymentStatus(allowed(text(row, headers, "payment status"), BisIsiService.PAYMENT_STATUSES, "1st Installment"));
                    operation.setAdvancePaymentStatus(optional(text(row, headers, "advance payment status"), "Pending"));
                    String sourceRemarks = text(row, headers, "remarks", "remark");
                    if (matchedState.isEmpty()) { String stateNote = state.isBlank() ? "Imported without a state" : "Imported state: " + state; operation.setRemarks(sourceRemarks.isBlank() ? stateNote : sourceRemarks + " | " + stateNote); }
                    else operation.setRemarks(sourceRemarks);
                    service.save(user, operation, null);
                    imported++;
                } catch (Exception exception) {
                    skipped++;
                    errors.add("Row " + (index + 1) + ": " + exception.getMessage());
                }
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to read Excel file: " + exception.getMessage());
        }
        return new Result(imported, skipped, errors.stream().limit(10).toList());
    }

    public byte[] export(User user, LocalDate startDate, LocalDate endDate, String procedure, Boolean extras,
                         Long engineer, Long creator, String status, String search) {
        List<BisIsiOperation> operations = new ArrayList<>();
        int page = 0;
        org.springframework.data.domain.Page<BisIsiOperation> result;
        do {
            result = service.list(user, startDate, endDate, procedure, extras, engineer, creator, status, search, page++);
            operations.addAll(result.getContent());
        } while (result.hasNext());
        Map<Long, State> stateMap = states.findAllById(operations.stream().map(BisIsiOperation::getStateId).filter(Objects::nonNull).collect(Collectors.toSet())).stream().collect(Collectors.toMap(State::getId, Function.identity()));
        Map<Long, User> userMap = service.creators().stream().collect(Collectors.toMap(User::getId, Function.identity(), (first, ignored) -> first));
        String[] columns = {"Date","Company Name","Client Name","Indian Standard","Contact Number","Client Email","Address","State","User ID","Project Status","Procedure","Payment Status","Advance Payment Status","Testing Status","Testing Person","Operating Person","CML Number","Created By","Assigned To","Remarks"};
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("BIS-ISI Operations"); sheet.createFreezePane(0,1);
            CellStyle headerStyle = workbook.createCellStyle(); headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex()); headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND); Font font = workbook.createFont(); font.setBold(true); font.setColor(IndexedColors.WHITE.getIndex()); headerStyle.setFont(font);
            Row header = sheet.createRow(0); for(int column=0;column<columns.length;column++){Cell cell=header.createCell(column);cell.setCellValue(columns[column]);cell.setCellStyle(headerStyle);}
            for(int index=0;index<operations.size();index++){BisIsiOperation o=operations.get(index);State state=stateMap.get(o.getStateId());User created=userMap.get(o.getCreatedBy()),assigned=userMap.get(o.getAssignedEngineerId());String[] values={safe(o.getOperationDate()),o.getCompanyName(),o.getClientName(),o.getIndianStandard(),o.getContactNumber(),o.getEmail(),o.getAddress(),state==null?"":state.getName(),o.getPortalUsername(),o.getProjectStatus(),o.getProcedure(),o.getPaymentStatus(),o.getAdvancePaymentStatus(),o.getTestingStatus(),o.getTestingPerson(),o.getOperatingPerson(),o.getCmlNumber(),created==null?"":created.getName(),assigned==null?"":assigned.getName(),o.getRemarks()};Row row=sheet.createRow(index+1);for(int column=0;column<values.length;column++)row.createCell(column).setCellValue(safe(values[column]));}
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0,Math.max(0,operations.size()),0,columns.length-1));for(int column=0;column<columns.length;column++){sheet.autoSizeColumn(column);sheet.setColumnWidth(column,Math.min(sheet.getColumnWidth(column)+512,15000));}workbook.write(output);return output.toByteArray();
        } catch(Exception exception){throw new IllegalStateException("Unable to export BIS-ISI operations",exception);}
    }

    private Map<String, Integer> headers(Row row) {
        if (row == null) throw new IllegalArgumentException("Excel header row is missing");
        Map<String, Integer> result = new HashMap<>();
        for (Cell cell : row) result.put(cell.toString().trim().toLowerCase(), cell.getColumnIndex());
        return result;
    }

    private String text(Row row, Map<String, Integer> headers, String... names) {
        for (String name : names) {
            Integer index = headers.get(name);
            if (index != null && row.getCell(index) != null)
                return new DataFormatter().formatCellValue(row.getCell(index)).trim();
        }
        return "";
    }

    private boolean blankRow(Row row){DataFormatter formatter=new DataFormatter();for(Cell cell:row)if(!formatter.formatCellValue(cell).trim().isBlank())return false;return true;}

    private String optional(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String allowed(String value,List<String> allowed,String fallback){if(value==null||value.isBlank())return fallback;return allowed.stream().filter(option->option.equalsIgnoreCase(value.trim())).findFirst().orElse(fallback);}

    private LocalDate date(String value,LocalDate fallback){if(value==null||value.isBlank())return fallback;for(DateTimeFormatter format:List.of(DateTimeFormatter.ISO_LOCAL_DATE,DateTimeFormatter.ofPattern("dd-MM-yyyy"),DateTimeFormatter.ofPattern("dd/MM/yyyy"))){try{return LocalDate.parse(value,format);}catch(Exception ignored){}}return fallback;}
    private String safe(Object value){if(value==null)return "";String text=String.valueOf(value).replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"," ");return text.length()>32767?text.substring(0,32767):text;}
}
