CREATE TABLE journal_files (
    id BIGSERIAL PRIMARY KEY,
    journal_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(127) NOT NULL,
    file_content BYTEA NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_journal_files_journal
        FOREIGN KEY (journal_id) REFERENCES journals (id) ON DELETE CASCADE,
    CONSTRAINT uq_journal_files_journal_name UNIQUE (journal_id, file_name),
    CONSTRAINT ck_journal_files_not_empty CHECK (octet_length(file_content) > 0),
    CONSTRAINT ck_journal_files_max_size CHECK (octet_length(file_content) <= 26214400)
);

CREATE INDEX idx_journal_files_journal_uploaded
    ON journal_files (journal_id, uploaded_at, file_name);