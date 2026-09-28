package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quotation_edit_history")
public class QuotationEditHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "testing_equipment_id", nullable = false) private Integer testingEquipmentId;
    @Column(name = "editor_name", nullable = false) private String editorName;
    @Column(name = "edited_at", nullable = false) private LocalDateTime editedAt;
    public QuotationEditHistory() { }
    public QuotationEditHistory(Integer testingEquipmentId, String editorName, LocalDateTime editedAt) { this.testingEquipmentId = testingEquipmentId; this.editorName = editorName; this.editedAt = editedAt; }
    public String getEditorName() { return editorName; }
    public LocalDateTime getEditedAt() { return editedAt; }
}
