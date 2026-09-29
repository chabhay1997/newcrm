package model.quotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "bee_quotation_fees")
public class BeeQuotationFee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "bee_quotation_id", nullable = false)
    public Long beeQuotationId;

    public String stepLabel;
    public String typeOfFee;
    public String amount;
    public String remarks;

    @Column(nullable = false)
    public Integer sortOrder = 0;

    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}