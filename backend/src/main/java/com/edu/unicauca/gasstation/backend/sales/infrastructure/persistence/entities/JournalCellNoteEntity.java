package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "journal_cell_notes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_journal_cell_notes_target",
                columnNames = {"journal_id", "step_number", "column_key", "field_key"}))
public class JournalCellNoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "journal_id", nullable = false)
    private Long journalId;

    @Column(name = "step_number", nullable = false)
    private Integer stepNumber;

    @Column(name = "column_key", nullable = false, length = 64)
    private String columnKey;

    @Column(name = "field_key", nullable = false, length = 64)
    private String fieldKey;

    @Column(name = "note", nullable = false, columnDefinition = "TEXT")
    private String note;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected JournalCellNoteEntity() {
    }

    public JournalCellNoteEntity(
            Long id,
            Long journalId,
            Integer stepNumber,
            String columnKey,
            String fieldKey,
            String note,
            LocalDateTime updatedAt) {
        this.id = id;
        this.journalId = journalId;
        this.stepNumber = stepNumber;
        this.columnKey = columnKey;
        this.fieldKey = fieldKey;
        this.note = note;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getJournalId() {
        return journalId;
    }

    public Integer getStepNumber() {
        return stepNumber;
    }

    public String getColumnKey() {
        return columnKey;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
