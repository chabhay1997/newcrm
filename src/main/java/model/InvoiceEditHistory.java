package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_edit_history")
public class InvoiceEditHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "editor_name", nullable = false)
    private String editorName;

    @Column(name = "edited_at", nullable = false)
    private LocalDateTime editedAt;

    public InvoiceEditHistory() { }
    public InvoiceEditHistory(Long invoiceId, String editorName, LocalDateTime editedAt) {
        this.invoiceId = invoiceId; this.editorName = editorName; this.editedAt = editedAt;
    }
    public Long getInvoiceId() { return invoiceId; }
    public String getEditorName() { return editorName; }
    public LocalDateTime getEditedAt() { return editedAt; }
}
