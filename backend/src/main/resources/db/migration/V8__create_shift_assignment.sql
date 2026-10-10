-- S1-27 · Monthly shift schedule (shifts module).
-- Which shift code each worker has each day. DESCANSO is stored as a row too, so "no row" means "not scheduled".

CREATE TABLE shift_assignment (
  id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  -- worker belongs to the workers module. The entity keeps worker_id as a plain UUID, so this FK only exists here.
  worker_id     UUID         NOT NULL CONSTRAINT fk_shift_assignment_worker REFERENCES worker (id),
  work_date     DATE         NOT NULL,
  shift_code_id UUID         NOT NULL CONSTRAINT fk_shift_assignment_shift_code REFERENCES shift_code (id),
  -- Optional reason for a change; base for future "novedades".
  note          VARCHAR(255),
  -- A worker has a single shift per day.
  CONSTRAINT uk_shift_assignment_worker_date UNIQUE (worker_id, work_date)
);

-- The schedule is always read by date range for every worker; the unique index starts with worker_id and does not help.
CREATE INDEX ix_shift_assignment_work_date ON shift_assignment (work_date);
