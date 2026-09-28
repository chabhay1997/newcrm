package service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import model.EmployeeApplicant;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmployeeApplicantPdfService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private final ObjectMapper objectMapper;

    public EmployeeApplicantPdfService(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    public byte[] generate(EmployeeApplicant applicant) throws Exception {
        StringBuilder html = new StringBuilder("""
                <!DOCTYPE html><html><head><meta charset='UTF-8'/><style>
                @page { size:A4; margin:18mm 13mm; } body{font-family:Arial,sans-serif;color:#19375f;font-size:9pt;padding-top:28mm} .letterhead{position:fixed;width:103%;height:102%;top:-20px;left:-10px;z-index:-2}.watermark{position:fixed;width:100%;top:35%;left:5%;opacity:.1;z-index:-1} h1{color:#0a438c;font-size:25pt;letter-spacing:2px;margin:4px 0}.pdf-section{page-break-inside:avoid;margin-top:18px}h2{color:#08428f;background:#e8f3ff;border:1px solid #c6def9;padding:7px 9px;font-size:12pt;margin:0;page-break-after:avoid}.kicker{color:#4270a8;font-weight:bold;letter-spacing:2px}.person{font-size:14pt;color:#285993;margin:8px 0 14px}.grid{width:100%;border-collapse:collapse}.grid td{width:50%;border:1px solid #d6e6f8;padding:7px;vertical-align:top}.label{display:block;color:#174c90;font-size:8pt;font-weight:bold;margin-bottom:3px}.value{color:#283f5c}.table{width:100%;border-collapse:collapse}.table th{background:#eef6ff;color:#174c90;text-align:left;padding:6px;border:1px solid #c6def9}.table td{padding:6px;border:1px solid #d6e6f8;vertical-align:top}.muted{color:#778da9;text-align:center}tr{page-break-inside:avoid}</style></head><body>
                """).append("<img class='watermark' src='").append(assetDataUri("static/evtl-watermark.svg", "image/svg+xml")).append("'/><img class='letterhead' src='").append(assetDataUri("static/letterhead.png", "image/png")).append("'/><div class='kicker'>EVTL INDIA · EMPLOYEE APPLICATION</div><h1>APPLICATION FORM</h1><div class='person'>")
                .append(text(applicant.getName())).append("</div>");
        section(html, "Job Details", fields(new String[][]{{"Position applied for", applicant.getPosition()}, {"Job reference no.", applicant.getJobRef()}, {"Preferred start date", date(applicant.getStartDate())}, {"Employment type", employment(applicant.getEmploymentType())}, {"Own laptop", yesNo(applicant.getOwnLaptop())}, {"Bond agreement", yesNo(applicant.getBond())}, {"Last / current salary", applicant.getLastSalary()}, {"Salary expectation", applicant.getSalaryExpect()}}));
        section(html, "Personal Information", fields(new String[][]{{"Full name", applicant.getName()}, {"Date of birth", date(applicant.getDob())}, {"Nationality", nationality(applicant.getNationality())}, {"National ID / passport", applicant.getNationalId()}, {"Phone", applicant.getPhone()}, {"Email", applicant.getEmail()}, {"Residential address", applicant.getAddress()}}));
        table(html, "Education &amp; Qualifications", new String[]{"Institute Name","Qualification","Board","Year","Percentage"}, rows(applicant.getInstituteName(), applicant.getQualification(), applicant.getBoard(), applicant.getYear(), applicant.getPercentage()));
        table(html, "Employment History", new String[]{"Employer Name","Job Title","Responsibilities","Employment Period"}, rows(applicant.getEmployerName(), applicant.getJobTitle(), applicant.getResponsibilities(), applicant.getEmploymentPeriod()));
        section(html, "Additional Information", fields(new String[][]{{"Reason for leaving", applicant.getReasonForLeave()}, {"Relevant skills", applicant.getRelevantSkill()}}));
        table(html, "Certifications", new String[]{"Course / Certification","Specialization","Date completed"}, rows(applicant.getCourseName(), applicant.getSpecialization(), applicant.getDateCompleted()));
        table(html, "References", new String[]{"Name","Designation","Relationship","Contact no.","Email"}, rows(applicant.getRefName(), applicant.getRefDesignation(), applicant.getRefRelationship(), applicant.getRefContact(), applicant.getRefEmail()));
        section(html, "Declaration", fields(new String[][]{{"Signature", applicant.getSignature()}, {"Application date", date(applicant.getApplicationDate())}}));
        html.append("</body></html>");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new PdfRendererBuilder().useFastMode().withHtmlContent(html.toString(), null).toStream(output).run();
        return output.toByteArray();
    }

    private static String fields(String[][] values) { StringBuilder out = new StringBuilder("<table class='grid'><tr>"); for (int i=0;i<values.length;i++) { if (i > 0 && i % 2 == 0) out.append("</tr><tr>"); out.append("<td><span class='label'>").append(text(values[i][0])).append("</span><span class='value'>").append(text(values[i][1])).append("</span></td>"); } if (values.length % 2 != 0) out.append("<td></td>"); return out.append("</tr></table>").toString(); }
    private static void section(StringBuilder html, String title, String contents) { html.append("<div class='pdf-section'><h2>").append(title).append("</h2>").append(contents).append("</div>"); }
    private static void table(StringBuilder html, String title, String[] headers, List<List<String>> data) { html.append("<div class='pdf-section'><h2>").append(title).append("</h2><table class='table'><tr>"); for(String header:headers) html.append("<th>").append(header).append("</th>"); html.append("</tr>"); if(data.isEmpty()) html.append("<tr><td class='muted' colspan='").append(headers.length).append("'>No details provided</td></tr>"); for(List<String> row:data){html.append("<tr>");for(String cell:row)html.append("<td>").append(text(cell)).append("</td>");html.append("</tr>");} html.append("</table></div>"); }
    private List<List<String>> rows(String... columns) { List<List<String>> arrays = new ArrayList<>(); int size=0; for(String column:columns){List<String> values=array(column);arrays.add(values);size=Math.max(size,values.size());} List<List<String>> rows=new ArrayList<>(); for(int i=0;i<size;i++){List<String> row=new ArrayList<>();for(List<String> column:arrays)row.add(i<column.size()?column.get(i):"");rows.add(row);}return rows; }
    private List<String> array(String value) { if(value==null||value.isBlank()) return List.of(); try{return objectMapper.readValue(value,new TypeReference<List<String>>(){});}catch(Exception ignored){return List.of(value);} }
    private static String assetDataUri(String path, String contentType) throws Exception { ClassPathResource asset = new ClassPathResource(path); return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(asset.getInputStream().readAllBytes()); }
    private static String date(java.time.LocalDate value){return value==null?"-":DATE.format(value);} private static String yesNo(Integer value){return value!=null&&value==1?"Yes":value!=null&&value==2?"No":"-";} private static String employment(Integer value){return value==null?"-":switch(value){case 1->"Full-time";case 2->"Part-time";case 3->"Contract";case 4->"Internship";default->"-";};} private static String nationality(String value){return "1".equals(value)?"Indian":"2".equals(value)?"Other":value;} private static String text(String value){return value==null||value.isBlank()?"-":value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\n","<br/>");}
}
