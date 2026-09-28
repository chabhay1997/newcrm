package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_applications")
public class EmployeeApplicant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String position;
    @Column(name = "job_ref")
    private String jobRef;
    @Column(name = "start_date")
    private LocalDate startDate;
    @Column(name = "employment_type")
    private Integer employmentType;
    @Column(name = "own_laptop")
    private Integer ownLaptop;
    private Integer bond;
    @Column(name = "last_salary")
    private String lastSalary;
    @Column(name = "salary_expect")
    private String salaryExpect;
    private String name;
    private LocalDate dob;
    private String nationality;
    @Column(name = "nation_pass_number")
    private String nationalId;
    private String phone;
    private String email;
    @Column(name = "residential_address")
    private String address;

    @Lob
    @Column(name = "institute_name", columnDefinition = "LONGTEXT")
    private String instituteName;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String qualification;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String board;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String year;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String percentage;
    @Lob
    @Column(name = "employer_name", columnDefinition = "LONGTEXT")
    private String employerName;
    @Lob
    @Column(name = "job_title", columnDefinition = "LONGTEXT")
    private String jobTitle;
    @Lob
    @Column(name = "responsibilities", columnDefinition = "LONGTEXT")
    private String responsibilities;
    @Lob
    @Column(name = "employment_period", columnDefinition = "LONGTEXT")
    private String employmentPeriod;
    @Lob
    @Column(name = "course_name", columnDefinition = "LONGTEXT")
    private String courseName;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String specialization;
    @Lob
    @Column(name = "date_completed", columnDefinition = "LONGTEXT")
    private String dateCompleted;
    @Lob
    @Column(name = "ref_name", columnDefinition = "LONGTEXT")
    private String refName;
    @Lob
    @Column(name = "ref_designation", columnDefinition = "LONGTEXT")
    private String refDesignation;
    @Lob
    @Column(name = "ref_relationship", columnDefinition = "LONGTEXT")
    private String refRelationship;
    @Lob
    @Column(name = "ref_contact", columnDefinition = "LONGTEXT")
    private String refContact;
    @Lob
    @Column(name = "ref_email", columnDefinition = "LONGTEXT")
    private String refEmail;

    @Lob
    @Column(name = "reason_for_leave", columnDefinition = "LONGTEXT")
    private String reasonForLeave;
    @Lob
    @Column(name = "relevant_skill", columnDefinition = "LONGTEXT")
    private String relevantSkill;
    private String signature;
    @Column(name = "application_date")
    private LocalDate applicationDate;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getJobRef() { return jobRef; }
    public void setJobRef(String jobRef) { this.jobRef = jobRef; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public Integer getEmploymentType() { return employmentType; }
    public void setEmploymentType(Integer employmentType) { this.employmentType = employmentType; }
    public Integer getOwnLaptop() { return ownLaptop; }
    public void setOwnLaptop(Integer ownLaptop) { this.ownLaptop = ownLaptop; }
    public Integer getBond() { return bond; }
    public void setBond(Integer bond) { this.bond = bond; }
    public String getLastSalary() { return lastSalary; }
    public void setLastSalary(String lastSalary) { this.lastSalary = lastSalary; }
    public String getSalaryExpect() { return salaryExpect; }
    public void setSalaryExpect(String salaryExpect) { this.salaryExpect = salaryExpect; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getInstituteName() { return instituteName; }
    public void setInstituteName(String value) { instituteName = value; }
    public String getQualification() { return qualification; }
    public void setQualification(String value) { qualification = value; }
    public String getBoard() { return board; }
    public void setBoard(String value) { board = value; }
    public String getYear() { return year; }
    public void setYear(String value) { year = value; }
    public String getPercentage() { return percentage; }
    public void setPercentage(String value) { percentage = value; }
    public String getEmployerName() { return employerName; }
    public void setEmployerName(String value) { employerName = value; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String value) { jobTitle = value; }
    public String getResponsibilities() { return responsibilities; }
    public void setResponsibilities(String value) { responsibilities = value; }
    public String getEmploymentPeriod() { return employmentPeriod; }
    public void setEmploymentPeriod(String value) { employmentPeriod = value; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String value) { courseName = value; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String value) { specialization = value; }
    public String getDateCompleted() { return dateCompleted; }
    public void setDateCompleted(String value) { dateCompleted = value; }
    public String getRefName() { return refName; }
    public void setRefName(String value) { refName = value; }
    public String getRefDesignation() { return refDesignation; }
    public void setRefDesignation(String value) { refDesignation = value; }
    public String getRefRelationship() { return refRelationship; }
    public void setRefRelationship(String value) { refRelationship = value; }
    public String getRefContact() { return refContact; }
    public void setRefContact(String value) { refContact = value; }
    public String getRefEmail() { return refEmail; }
    public void setRefEmail(String value) { refEmail = value; }
    public String getReasonForLeave() { return reasonForLeave; }
    public void setReasonForLeave(String value) { reasonForLeave = value; }
    public String getRelevantSkill() { return relevantSkill; }
    public void setRelevantSkill(String value) { relevantSkill = value; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public LocalDate getApplicationDate() { return applicationDate; }
    public void setApplicationDate(LocalDate applicationDate) { this.applicationDate = applicationDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
