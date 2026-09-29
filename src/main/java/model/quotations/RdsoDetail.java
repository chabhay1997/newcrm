package model.quotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "r_d_s_o_details")
public class RdsoDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    // points to r_d_s_o_s.id
    @Column(name = "r_d_s_o_fk_id", nullable = false) public Long rdsoFkId;

    @Column(name = "s_no") public Integer sNo;
    @Column(name = "description", columnDefinition = "TEXT") public String description;
    @Column(name = "timeline", columnDefinition = "TEXT") public String timeline;
    @Column(name = "charges") public String charges;
    @Column(name = "extra_alias") public String extraAlias;

    @Column(name = "created_at", updatable = false) public LocalDateTime createdAt;
    @Column(name = "updated_at") public LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}