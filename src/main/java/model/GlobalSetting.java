package model;

import jakarta.persistence.*;

@Entity
@Table(name = "global")
public class GlobalSetting {

    @Id
    private Long id;

    @Column(name = "bank_choose")
    private Long bankChoose;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBankChoose() { return bankChoose; }
    public void setBankChoose(Long bankChoose) { this.bankChoose = bankChoose; }
}