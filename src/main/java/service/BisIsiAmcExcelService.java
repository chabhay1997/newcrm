package service;

import model.BisIsiOperation;
import model.BisIsiAmcQuotation;
import model.User;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import repository.BisIsiAmcQuotationRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BisIsiAmcExcelService {
    private final BisIsiService operations;
    private final BisIsiAmcQuotationRepository quotations;

    public BisIsiAmcExcelService(BisIsiService operations, BisIsiAmcQuotationRepository quotations) {
        this.operations = operations;
        this.quotations = quotations;
    }

    public byte[] export(User user, String search) {
        List<BisIsiOperation> records = operations.allAmc(user, search);
        Map<Long, BisIsiAmcQuotation> quoteByOperation = records.isEmpty() ? Map.of() : quotations
                .findByOperationIdIn(records.stream().map(BisIsiOperation::getId).toList()).stream()
                .collect(Collectors.toMap(BisIsiAmcQuotation::getOperationId, quote -> quote));
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("BIS ISI-AMC");
            String[] columns = {"SN", "Company", "Client", "IS No.", "Product", "CML No.", "Grant Date", "Validity"};
            Row header = sheet.createRow(0);
            for (int index = 0; index < columns.length; index++) header.createCell(index).setCellValue(columns[index]);
            for (int index = 0; index < records.size(); index++) {
                BisIsiOperation operation = records.get(index);
                BisIsiAmcQuotation quote = quoteByOperation.get(operation.getId());
                Row row = sheet.createRow(index + 1);
                row.createCell(0).setCellValue(index + 1);
                row.createCell(1).setCellValue(display(operation.getCompanyName()));
                row.createCell(2).setCellValue(display(operation.getClientName()));
                row.createCell(3).setCellValue(display(operation.getIndianStandard()));
                row.createCell(4).setCellValue(quote == null ? "Not added" : display(quote.getProduct()));
                row.createCell(5).setCellValue(display(quote != null && quote.getCmlNumber() != null ? quote.getCmlNumber() : operation.getCmlNumber()));
                row.createCell(6).setCellValue(operation.getLicenceDate() == null ? "N/A" : operation.getLicenceDate().toString());
                row.createCell(7).setCellValue(quote == null || quote.getLicenceValidityDate() == null ? "N/A" : quote.getLicenceValidityDate().toString());
            }
            for (int index = 0; index < columns.length; index++) sheet.setColumnWidth(index, index == 1 ? 10000 : 5000);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to export BIS ISI-AMC records", exception);
        }
    }

    private String display(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }
}
