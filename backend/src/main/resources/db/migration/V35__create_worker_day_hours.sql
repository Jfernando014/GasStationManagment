-- Hours worked by each worker on each closed day, kept as a fixed copy of what the day staff said when the journal
-- closed, and the processing state of each date. The hours are the sum of the segments of the code worked, with no
-- rest hour, surcharge or holiday applied.

CREATE TABLE worker_day_hours (
  id                    BIGSERIAL    PRIMARY KEY,
  work_date             DATE         NOT NULL,
  worker_id             UUID         NOT NULL CONSTRAINT fk_worker_day_hours_worker REFERENCES worker (id),
  -- Names are copied so the record keeps what was shown that day, even if the worker is edited later.
  worker_name           VARCHAR(150) NOT NULL,
  -- Codes and role have the length of shift_code.code and role.name. Without a worked code (absent) they are NULL.
  worked_code           VARCHAR(255),
  grid_code             VARCHAR(255) NOT NULL,
  day_role              VARCHAR(255),
  total_hours           INTEGER      NOT NULL
    CONSTRAINT ck_worker_day_hours_total CHECK (total_hours >= 0),
  novelty_type          VARCHAR(20)  NOT NULL
    CONSTRAINT ck_worker_day_hours_novelty CHECK (novelty_type IN ('NONE', 'REPLACEMENT', 'ABSENCE', 'SHIFT_CHANGE', 'SWAP')),
  replaced_worker_id    UUID         CONSTRAINT fk_worker_day_hours_replaced REFERENCES worker (id),
  replaced_worker_name  VARCHAR(150),
  reason                VARCHAR(255),
  -- Journal that closed the day, as sales reported it; no foreign key because modules do not share tables.
  journal_id            BIGINT,
  -- False when the staff of the day was read before its novelties could be applied.
  novelties_applied     BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_worker_day_hours_worker_date UNIQUE (work_date, worker_id)
);

CREATE INDEX ix_worker_day_hours_worker_date ON worker_day_hours (worker_id, work_date);

CREATE TABLE worker_day_hours_segment (
  day_hours_id BIGINT  NOT NULL CONSTRAINT fk_worker_day_hours_segment REFERENCES worker_day_hours (id) ON DELETE CASCADE,
  position     INTEGER NOT NULL CONSTRAINT ck_worker_day_hours_segment_position CHECK (position >= 0),
  start_hour   INTEGER NOT NULL CONSTRAINT ck_worker_day_hours_segment_start CHECK (start_hour BETWEEN 0 AND 23),
  end_hour     INTEGER NOT NULL CONSTRAINT ck_worker_day_hours_segment_end CHECK (end_hour BETWEEN 1 AND 24),
  hours        INTEGER NOT NULL CONSTRAINT ck_worker_day_hours_segment_hours CHECK (hours BETWEEN 1 AND 24),
  PRIMARY KEY (day_hours_id, position)
);

CREATE TABLE worker_hours_sync (
  work_date         DATE         PRIMARY KEY,
  -- Data of the closed journal as sales reported it; empty when the date was processed by hand with no closing
  -- announced.
  journal_id        BIGINT,
  consecutive       BIGINT,
  closed_at         TIMESTAMP,
  status            VARCHAR(12)  NOT NULL
    CONSTRAINT ck_worker_hours_sync_status CHECK (status IN ('PROCESSED', 'PENDING')),
  novelties_applied BOOLEAN      NOT NULL DEFAULT FALSE,
  attempts          INTEGER      NOT NULL DEFAULT 0,
  last_error        VARCHAR(500),
  processed_at      TIMESTAMPTZ,
  updated_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
