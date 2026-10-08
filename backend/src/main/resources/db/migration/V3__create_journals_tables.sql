CREATE TABLE journals (
    id BIGSERIAL PRIMARY KEY,
    journal_date DATE NOT NULL UNIQUE,
    consecutive BIGINT NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE journal_steps (
    id BIGSERIAL PRIMARY KEY,
    journal_id BIGINT NOT NULL,
    step_number INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_journal FOREIGN KEY (journal_id) REFERENCES journals (id) ON DELETE CASCADE,
    CONSTRAINT uq_journal_step UNIQUE (journal_id, step_number)
);
