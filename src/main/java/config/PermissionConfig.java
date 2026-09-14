package config;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PermissionConfig {

    private PermissionConfig() {
    }

    public static Map<String, Map<String, String>> groups() {
        Map<String, Map<String, String>> permissions = new LinkedHashMap<>();
        permissions.put("General Permissions", group(
                "user", "Users", "client", "Clients", "all_lead", "All Leads", "leads_delete", "Leads Delete Button",
                "leads_dashboard", "Leads Dashboard", "salesVendor", "Sales Vendor", "challan", "Challan", "preModal", "Pre Module",
                "notifications", "Notification", "notes", "Sticky Notes", "ip_access", "Ip Access", "company_document", "Company Document",
                "company_profile_access", "Company Profile Access", "service_contract", "Service Contract", "large_scale_project", "Large Project"));
        permissions.put("Lab Equipment", group(
                "isi_checklist", "ISI Checklist", "testingequipment", "Lab Equipment Quotation", "equipTermsConditions", "Equipment Terms-Conditions",
                "labPurchase", "Lab Equipment Purchase", "equipList", "Equipment List", "preInspection", "PreInspection"));
        permissions.put("Employee Details", group(
                "emp_info", "Employee Information", "emp_per", "Employee Performance", "emp_per_info", "Employee Personal Information",
                "emp_applicant", "Employee Application Form", "emp", "Employee", "empTerminate", "Employee Terminate", "empAsset", "Employee Asset",
                "offer", "Offer Letter", "probation", "Probation Period", "career", "Career", "vacancy", "Vacancy", "attendance", "Attendence", "reports", "User Reports"));
        permissions.put("Operation", group(
                "operation", "Bis-IsI (Domestic)", "amc", "AMC", "bisCrs", "Bis-Crs (Renewal)", "services", "Bis-Crs", "trademark", "Trademark",
                "cdsco", "CDSCO", "rdso_operation", "RDSO Operation", "agmark_operation", "Agmark Operation", "fmcs", "FMCS", "epr_reg", "EPR Registration",
                "epr_post", "EPR-POST Registration", "nsws", "NSWS", "iso_certificate", "ISO Certificate", "iso", "ISO", "gem", "GEM", "bee", "Bee",
                "tec", "Tec", "wmi", "Wmi", "waste", "Waste Collection", "test_status", "Testing Status", "lab_payment", "Lab Payment", "saral", "Saral-Sanchar",
                "fi_visit", "Foreign Inspection Visit", "licRenewal", "License Renewal", "schemeXOTR", "SchemeXOTR", "noc", "Noc", "schemeXCRS", "Scheme-X FMCS", "technicalSIT", "Technical SIT"));
        permissions.put("Logistic", group("logistic", "Logistic Details", "vendor_detail", "Vendor Details", "logistic_payment", "Logistics Payment"));
        permissions.put("Account", group("invoiceUSD", "Invoice USD"));
        permissions.put("Vendor", group("testing_lab", "Testing Lab", "vendor_report", "Vendor Report", "po", "Purchase Order", "expense", "Expense", "vendorData", "Vendor Data"));
        permissions.put("For Training", group("training", "Basic Training", "faqMarketing", "Training for Marketing", "faqOperation", "Training for Operation", "upload_bulk_doc", "Vendor Bulk Document"));
        permissions.put("Other", group("bulk_email", "Bulk Email", "notify", "Notification", "log", "Log", "cache", "Cache Clear"));
        permissions.put("Trainee Module", group("Marketing", "Marketing", "Developer", "Developer", "Operation", "Operation", "Digital Marketing", "Digital Marketing", "Inspection", "Inspection", "BIS-ISI", "BIS-ISI", "BIS-FMCS", "BIS-FMCS", "Scheme-X", "Scheme-X"));
        return permissions;
    }

    private static Map<String, String> group(String... values) {
        Map<String, String> group = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            group.put(values[i], values[i + 1]);
        }
        return group;
    }
}
