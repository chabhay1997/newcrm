package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "challan_edit_history")
public class ChallanEditHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "challan_id", nullable = false)
    private Long challanId;

    @Column(name = "editor_name", nullable = false)
    private String editorName;

    @Column(name = "edited_at", nullable = false)
    private LocalDateTime editedAt;

    public ChallanEditHistory() { }

    public ChallanEditHistory(Long challanId, String editorName, LocalDateTime editedAt) {
        this.challanId = challanId;
        this.editorName = editorName;
        this.editedAt = editedAt;
    }

    public Long getChallanId() { return challanId; }
    public String getEditorName() { return editorName; }
    public LocalDateTime getEditedAt() { return editedAt; }
}
