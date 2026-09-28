package config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Ensures the BIS-ISI tables exist in installations that predate this module. */
@Component
@ConditionalOnProperty(name = "spring.datasource.driver-class-name", havingValue = "com.mysql.cj.jdbc.Driver")
public class BisIsiSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public BisIsiSchemaInitializer(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS bis_isi_operations (
              id BIGINT NOT NULL AUTO_INCREMENT,
              operation_date DATE,
              company_name VARCHAR(255) NOT NULL,
              client_name VARCHAR(255) NOT NULL,
              indian_standard VARCHAR(80) NOT NULL,
              contact_number VARCHAR(30) NOT NULL,
              alternate_contact_number VARCHAR(30), email VARCHAR(255), address TEXT, state_id BIGINT NOT NULL,
              portal_username VARCHAR(255), portal_password VARCHAR(255),
              evtl_email VARCHAR(255), evtl_email_passcode VARCHAR(255),
              project_status VARCHAR(255) NOT NULL, procedure_code VARCHAR(10) NOT NULL,
              payment_status VARCHAR(255), advance_payment_status VARCHAR(255),
              payment_installments INT, total_amount DECIMAL(14,2),
              testing_deadline DATE, testing_status VARCHAR(255), testing_person VARCHAR(255),
              operating_person VARCHAR(255), application_deadline DATE, licence_deadline DATE,
              target_date DATE, target_final_exception_date DATE, final_date DATE, cml_number VARCHAR(255),
              licence_number VARCHAR(255), licence_date DATE,
              created_by BIGINT NOT NULL, assigned_engineer_id BIGINT, assigned_engineer_name VARCHAR(255),
              remarks TEXT, inclusion_enabled BIT NOT NULL DEFAULT 0,
              renewal_enabled BIT NOT NULL DEFAULT 0,
              brand_inclusion_enabled BIT NOT NULL DEFAULT 0,
              third_party_visit_enabled BIT NOT NULL DEFAULT 0,
              sit_enabled BIT NOT NULL DEFAULT 0, sit_status VARCHAR(255),
              sit_quarter VARCHAR(255), extra_service VARCHAR(255), extra_service_notes TEXT,
              created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
              PRIMARY KEY (id),
              CONSTRAINT uk_bis_company_standard UNIQUE (company_name, indian_standard),
              INDEX idx_bis_assignee (assigned_engineer_id),
              INDEX idx_bis_status (project_status),
              INDEX idx_bis_created_at (created_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS bis_pre_inspections (
              id BIGINT NOT NULL AUTO_INCREMENT, operation_id BIGINT NOT NULL,
              company_name VARCHAR(255), contact_number VARCHAR(255), location VARCHAR(255),
              indian_standard VARCHAR(255), procedure_code VARCHAR(255), target_date DATE,
              final_date DATE, assigned_engineer_id BIGINT,
              PRIMARY KEY (id), UNIQUE KEY uk_preinspection_operation (operation_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS isi_checklists (
              id BIGINT NOT NULL AUTO_INCREMENT, operation_id BIGINT NOT NULL,
              company_name VARCHAR(255), client_name VARCHAR(255), indian_standard VARCHAR(255),
              contact_number VARCHAR(255), location VARCHAR(255), procedure_code VARCHAR(255),
              assigned_engineer_id BIGINT, created_by BIGINT NOT NULL,
              PRIMARY KEY (id), UNIQUE KEY uk_checklist_operation (operation_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
    }
}
