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
import java.sql.ResultSetMetaData;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CdscoExcelService {
    public record ImportResult(int imported, int duplicates, int invalid, List<String> errors) {}
    private static final String[] HEADERS = {"Name", "Email", "Contact", "User ID", "Payment Status", "Date"};
    private final JdbcTemplate jdbc;

    public CdscoExcelService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public byte[] export(List<Map<String,Object>> records) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("CDSCO"); sheet.createFreezePane(0, 1);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont(); font.setBold(true); font.setColor(IndexedColors.WHITE.getIndex()); headerStyle.setFont(font);
            Row header = sheet.createRow(0);
            for (int column = 0; column < HEADERS.length; column++) { Cell cell=header.createCell(column); cell.setCellValue(HEADERS[column]); cell.setCellStyle(headerStyle); }
            for (int index = 0; index < records.size(); index++) {
                Map<String,Object> record = records.get(index); Row row = sheet.createRow(index + 1);
                Object[] values = {record.get("name"), record.get("email"), record.get("contact"), record.get("userId"), record.get("status"), record.get("date")};
                for (int column = 0; column < values.length; column++) row.createCell(column).setCellValue(safe(values[column]));
            }
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, records.size()), 0, HEADERS.length - 1));
            for (int column=0; column<HEADERS.length; column++) { sheet.autoSizeColumn(column); sheet.setColumnWidth(column, Math.min(sheet.getColumnWidth(column)+600, 16000)); }
            workbook.write(output); return output.toByteArray();
        } catch (Exception exception) { throw new IllegalStateException("Unable to export CDSCO records", exception); }
    }

    @Transactional
    public ImportResult importFile(User user, MultipartFile file) {
        validate(file);
        Set<String> columns = tableColumns();
        String nameColumn = column(columns, "name", "company_name", "applicant_name", "client_name");
        if (nameColumn == null) throw new IllegalArgumentException("The cdscos table does not contain a supported name column.");
        Set<String> known = new HashSet<>();
        jdbc.queryForList("select * from cdscos").forEach(row -> known.add(key(row)));
        int imported=0, duplicates=0, invalid=0; List<String> errors=new ArrayList<>();
        try (InputStream input=file.getInputStream(); Workbook workbook=WorkbookFactory.create(input)) {
            Sheet sheet=workbook.getSheetAt(0); Map<String,Integer> headers=headers(sheet.getRow(sheet.getFirstRowNum()));
            require(headers,"name","company name","applicant name");
            for(int index=sheet.getFirstRowNum()+1;index<=sheet.getLastRowNum();index++) {
                Row row=sheet.getRow(index); if(row==null||blank(row)) continue;
                try {
                    String name=text(row,headers,"name","company name","applicant name");
                    if(name.isBlank()) throw new IllegalArgumentException("Name is required");
                    String email=text(row,headers,"email","email id","mail id");
                    String contact=text(row,headers,"contact","contact no","phone","mobile");
                    String userId=text(row,headers,"user id","userid");
                    String payment=text(row,headers,"payment status","status");
                    LocalDate date=date(row,headers,"date","application date","created date");
                    Map<String,Object> candidate=new LinkedHashMap<>(); candidate.put(nameColumn,name);
                    put(candidate,column(columns,"email","email_id","mail_id","mail"),email);
                    put(candidate,column(columns,"contact","contact_no","contact_number","phone","mobile"),contact);
                    put(candidate,column(columns,"user_id","userid","created_by"),userId.isBlank()?user.getId():userId);
                    put(candidate,column(columns,"payment_status","paymentstatus"),payment);
                    put(candidate,column(columns,"date","application_date","created_date"),date);
                    if(columns.contains("created_at")) candidate.put("created_at",new java.sql.Timestamp(System.currentTimeMillis()));
                    if(columns.contains("updated_at")) candidate.put("updated_at",new java.sql.Timestamp(System.currentTimeMillis()));
                    if(!known.add(key(candidate))){duplicates++;continue;}
                    String names=String.join(",",candidate.keySet()); String marks=String.join(",",Collections.nCopies(candidate.size(),"?"));
                    jdbc.update("insert into cdscos ("+names+") values ("+marks+")",candidate.values().toArray()); imported++;
                } catch(Exception exception){invalid++;if(errors.size()<10)errors.add("Row "+(index+1)+": "+exception.getMessage());}
            }
        } catch(IllegalArgumentException exception){throw exception;} catch(Exception exception){throw new IllegalArgumentException("Unable to read Excel file: "+exception.getMessage());}
        return new ImportResult(imported,duplicates,invalid,errors);
    }

    private Set<String> tableColumns(){return jdbc.query("select * from cdscos limit 0",rs->{Set<String> result=new LinkedHashSet<>();ResultSetMetaData meta=rs.getMetaData();for(int i=1;i<=meta.getColumnCount();i++)result.add(meta.getColumnLabel(i).toLowerCase(Locale.ROOT));return result;});}
    private String column(Set<String> columns,String...choices){for(String choice:choices)if(columns.contains(choice))return choice;return null;}
    private void put(Map<String,Object> target,String key,Object value){if(key!=null&&value!=null&&!value.toString().isBlank())target.put(key,value);}
    private String key(Map<String,Object> row){return normalize(value(row,"name","company_name","applicant_name","client_name"))+"|"+normalize(value(row,"email","email_id","mail_id"))+"|"+normalize(value(row,"contact","contact_no","phone","mobile"))+"|"+normalize(value(row,"date","application_date","created_at"));}
    private String value(Map<String,Object> row,String...keys){for(var entry:row.entrySet())for(String key:keys)if(entry.getKey().equalsIgnoreCase(key)&&entry.getValue()!=null)return entry.getValue().toString();return "";}
    private void validate(MultipartFile file){if(file==null||file.isEmpty())throw new IllegalArgumentException("Choose an Excel file to import.");String name=Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);if(!name.endsWith(".xlsx")&&!name.endsWith(".xls"))throw new IllegalArgumentException("Only .xlsx and .xls files can be imported.");}
    private Map<String,Integer> headers(Row row){if(row==null)throw new IllegalArgumentException("Excel header row is missing.");Map<String,Integer> result=new HashMap<>();DataFormatter formatter=new DataFormatter();for(Cell cell:row)result.put(normalize(cellString(formatter,cell)),cell.getColumnIndex());return result;}
    private void require(Map<String,Integer> headers,String...names){for(String name:names)if(headers.containsKey(normalize(name)))return;throw new IllegalArgumentException("Missing required Excel column: "+names[0]);}
    private String text(Row row,Map<String,Integer> headers,String...names){for(String name:names){Integer index=headers.get(normalize(name));if(index!=null&&row.getCell(index)!=null)return new DataFormatter().formatCellValue(row.getCell(index)).trim();}return "";}
    private LocalDate date(Row row,Map<String,Integer> headers,String...names){for(String name:names){Integer index=headers.get(normalize(name));if(index==null)continue;Cell cell=row.getCell(index);if(cell==null)return null;if(cell.getCellType()==CellType.NUMERIC&&DateUtil.isCellDateFormatted(cell))return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();String value=new DataFormatter().formatCellValue(cell).trim();if(value.isBlank())return null;for(DateTimeFormatter format:List.of(DateTimeFormatter.ISO_LOCAL_DATE,DateTimeFormatter.ofPattern("dd/MM/uuuu"),DateTimeFormatter.ofPattern("dd-MM-uuuu")))try{return LocalDate.parse(value,format);}catch(Exception ignored){}throw new IllegalArgumentException("Date must use YYYY-MM-DD or DD/MM/YYYY");}return null;}
    private boolean blank(Row row){DataFormatter formatter=new DataFormatter();for(Cell cell:row)if(!formatter.formatCellValue(cell).trim().isBlank())return false;return true;}
    private String cellString(DataFormatter formatter,Cell cell){return formatter.formatCellValue(cell);}
    private String normalize(String value){return value==null?"":value.trim().toLowerCase(Locale.ROOT).replaceAll("[._]","").replaceAll("\\s+"," ");}
    private String safe(Object value){return value==null||"N/A".equals(value.toString())?"":value.toString();}
}
