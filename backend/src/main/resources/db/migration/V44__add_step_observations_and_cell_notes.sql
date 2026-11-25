-- Per-step operational observations and Excel-style cell notes (SR-08 / SCRUM-118).

ALTER TABLE journal_steps
    ADD COLUMN IF NOT EXISTS observations TEXT;

CREATE TABLE IF NOT EXISTS journal_cell_notes (
    id BIGSERIAL PRIMARY KEY,
    journal_id BIGINT NOT NULL,
    step_number INTEGER NOT NULL,
    column_key VARCHAR(64) NOT NULL,
    field_key VARCHAR(64) NOT NULL,
    note TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_journal_cell_notes_journal
        FOREIGN KEY (journal_id) REFERENCES journals (id) ON DELETE CASCADE,
    CONSTRAINT uq_journal_cell_notes_target
        UNIQUE (journal_id, step_number, column_key, field_key)
);

CREATE INDEX IF NOT EXISTS idx_journal_cell_notes_journal_step
    ON journal_cell_notes (journal_id, step_number);
