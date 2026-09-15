package service;

import model.Challan;
import model.State;
import model.User;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.ChallanRepository;
import repository.StateRepository;
import repository.UserRepository;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChallanExcelExportService {
    private static final String[] HEADERS = {"S No.", "Challan No.", "Client Name", "Client Number",
            "Item Name", "Brand Name", "State", "Qty", "Amount", "GST (%)", "Total Amount", "Date",
            "Sample Return Date", "Address", "Pincode", "Remark", "Uploads", "Created By", "Created At"};
    private final ChallanRepository challanRepository;
    private final UserRepository userRepository;
    private final StateRepository stateRepository;

    public ChallanExcelExportService(ChallanRepository challanRepository, UserRepository userRepository,
                                     StateRepository stateRepository) {
        this.challanRepository = challanRepository;
        this.userRepository = userRepository;
        this.stateRepository = stateRepository;
    }

    @Transactional(readOnly = true)
    public byte[] exportAll() {
        return exportFiltered("", null, null);
    }

    @Transactional(readOnly = true)
    public byte[] exportFiltered(String query, LocalDate startDate, LocalDate endDate) {
        String search = query == null ? "" : query.trim();
        var challans = search.isBlank() && startDate == null && endDate == null
                ? challanRepository.findAll(Sort.by("id").ascending())
                : challanRepository.findFiltered(search, startDate, endDate, Sort.by("id").ascending());
        Map<Long, User> users = userRepository.findAllById(challans.stream().map(Challan::getCreatedBy)
                        .filter(java.util.Objects::nonNull).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, State> states = stateRepository.findAllById(challans.stream().map(Challan::getStateId)
                        .filter(java.util.Objects::nonNull).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(State::getId, Function.identity()));

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Challans");
            sheet.createFreezePane(0, 1);
            CellStyle headerStyle = headerStyle(workbook);
            Row header = sheet.createRow(0);
            for (int column = 0; column < HEADERS.length; column++) {
                header.createCell(column).setCellValue(HEADERS[column]);
                header.getCell(column).setCellStyle(headerStyle);
            }
            for (int index = 0; index < challans.size(); index++) {
                Challan c = challans.get(index);
                User creator = c.getCreatedBy() == null ? null : users.get(c.getCreatedBy());
                State state = c.getStateId() == null ? null : states.get(c.getStateId());
                Row row = sheet.createRow(index + 1);
                String[] values = {String.valueOf(index + 1), c.getChallanNo(), c.getClientName(), c.getClientNumber(),
                        c.getItemName(), c.getBrandName(), state == null ? null : state.getName(), c.getQty(),
                        c.getAmount(), c.getGst(), c.getTotalAmount(), format(c.getDate()), format(c.getSampleReturnDate()),
                        c.getAddress(), c.getPincode(), c.getRemark(), c.getUpload(), creator == null ? null : creator.getName(),
                        c.getCreatedAt() == null ? null : c.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))};
                for (int column = 0; column < values.length; column++) row.createCell(column).setCellValue(safe(values[column]));
            }
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, challans.size()), 0, HEADERS.length - 1));
            for (int column = 0; column < HEADERS.length; column++) {
                sheet.autoSizeColumn(column);
                sheet.setColumnWidth(column, Math.min(sheet.getColumnWidth(column) + 512, 15_000));
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to export Challans to Excel", exception);
        }
    }

    private CellStyle headerStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    private String format(java.time.LocalDate date) { return date == null ? "" : date.toString(); }
    private String safe(String value) {
        if (value == null) return "";
        String cleaned = value.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", " ");
        return cleaned.length() > 32_767 ? cleaned.substring(0, 32_767) : cleaned;
    }
}
