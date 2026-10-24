-- Novelties of the day staff (step 1 of the journal, shifts module).
-- The grid (shift_assignment) is never modified: a novelty keeps the original code of the grid and the code actually
-- worked. A cancelled novelty is kept for traceability and is no longer applied.

CREATE TABLE shift_novelty (
  id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  -- Operational date of the journal (NOCHE of day D belongs to D).
  work_date              DATE         NOT NULL,
  type                   VARCHAR(20)  NOT NULL
    CONSTRAINT ck_shift_novelty_type CHECK (type IN ('REPLACEMENT', 'ABSENCE', 'SHIFT_CHANGE', 'SWAP')),
  -- Worker whose grid code is original_shift_code: the absent one in REPLACEMENT and ABSENCE.
  worker_id              UUID         NOT NULL CONSTRAINT fk_shift_novelty_worker REFERENCES worker (id),
  -- The replacement in REPLACEMENT, the other worker in SWAP; NULL otherwise.
  counterpart_worker_id  UUID         CONSTRAINT fk_shift_novelty_counterpart REFERENCES worker (id),
  -- Code of worker_id in the grid; DESCANSO when the worker had no working code.
  original_shift_code_id UUID         NOT NULL CONSTRAINT fk_shift_novelty_original_code REFERENCES shift_code (id),
  -- Code actually worked: by the replacement (REPLACEMENT), by worker_id (SHIFT_CHANGE, SWAP); NULL in ABSENCE.
  final_shift_code_id    UUID         CONSTRAINT fk_shift_novelty_final_code REFERENCES shift_code (id),
  -- A code of another role was accepted on purpose.
  exceptional_coverage   BOOLEAN      NOT NULL DEFAULT FALSE,
  reason                 VARCHAR(255) NOT NULL
    CONSTRAINT ck_shift_novelty_reason CHECK (length(btrim(reason)) BETWEEN 3 AND 255),
  created_at             TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  cancelled_at           TIMESTAMPTZ,

  CONSTRAINT ck_shift_novelty_counterpart CHECK (
    (type IN ('REPLACEMENT', 'SWAP') AND counterpart_worker_id IS NOT NULL AND counterpart_worker_id <> worker_id)
    OR (type IN ('ABSENCE', 'SHIFT_CHANGE') AND counterpart_worker_id IS NULL)
  ),
  CONSTRAINT ck_shift_novelty_final_code CHECK (
    (type = 'ABSENCE' AND final_shift_code_id IS NULL)
    OR (type <> 'ABSENCE' AND final_shift_code_id IS NOT NULL)
  )
);

-- Novelties are always read by date range, like the grid.
CREATE INDEX ix_shift_novelty_work_date ON shift_novelty (work_date);
