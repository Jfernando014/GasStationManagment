-- Parameters of the fortnightly overtime sheet, kept as versions with a validity date (a change is a new version,
-- never an edit), and the manual adjustments of each sheet. The sheet itself is not stored: it is worked out from
-- the hours kept per day.

CREATE TABLE worker_overtime_parameter (
  id                   BIGSERIAL    PRIMARY KEY,
  valid_from           DATE         NOT NULL,
  night_start_hour     INTEGER      NOT NULL
    CONSTRAINT ck_worker_overtime_parameter_night CHECK (night_start_hour BETWEEN 12 AND 23),
  rest_hours           INTEGER      NOT NULL
    CONSTRAINT ck_worker_overtime_parameter_rest CHECK (rest_hours BETWEEN 0 AND 4),
  rest_min_shift_hours INTEGER      NOT NULL
    CONSTRAINT ck_worker_overtime_parameter_rest_min CHECK (rest_min_shift_hours BETWEEN 1 AND 24),
  fixed_hedo_hours     INTEGER      NOT NULL
    CONSTRAINT ck_worker_overtime_parameter_hedo CHECK (fixed_hedo_hours BETWEEN 0 AND 80),
  created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by           VARCHAR(100),
  CONSTRAINT uq_worker_overtime_parameter_valid_from UNIQUE (valid_from)
);

-- Roles that have the fixed overtime line on in a version; a role not listed has it off. The name has the length of
-- role.name, and there is no foreign key because roles belong to the shifts module.
CREATE TABLE worker_overtime_parameter_role (
  parameter_id BIGINT       NOT NULL
    CONSTRAINT fk_worker_overtime_parameter_role REFERENCES worker_overtime_parameter (id) ON DELETE CASCADE,
  role_name    VARCHAR(255) NOT NULL,
  PRIMARY KEY (parameter_id, role_name)
);

INSERT INTO worker_overtime_parameter
  (valid_from, night_start_hour, rest_hours, rest_min_shift_hours, fixed_hedo_hours, created_by)
VALUES (DATE '2025-01-01', 19, 1, 12, 8, 'initial values');

CREATE TABLE worker_overtime_adjustment (
  id              BIGSERIAL    PRIMARY KEY,
  worker_id       UUID         NOT NULL CONSTRAINT fk_worker_overtime_adjustment_worker REFERENCES worker (id),
  -- First day of the fortnight (the 1st or the 16th) the adjustment belongs to; nothing carries over.
  fortnight_start DATE         NOT NULL
    CONSTRAINT ck_worker_overtime_adjustment_fortnight CHECK (EXTRACT(DAY FROM fortnight_start) IN (1, 16)),
  category        VARCHAR(10)  NOT NULL
    CONSTRAINT ck_worker_overtime_adjustment_category
      CHECK (category IN ('RDF', 'RNO', 'RNF', 'HEDO', 'HENO', 'HEDF', 'HENF')),
  direction       VARCHAR(10)  NOT NULL
    CONSTRAINT ck_worker_overtime_adjustment_direction CHECK (direction IN ('ADD', 'SUBTRACT')),
  hours           INTEGER      NOT NULL
    CONSTRAINT ck_worker_overtime_adjustment_hours CHECK (hours > 0),
  reason          VARCHAR(255) NOT NULL
    CONSTRAINT ck_worker_overtime_adjustment_reason CHECK (length(btrim(reason)) BETWEEN 3 AND 255),
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by      VARCHAR(100)
);

CREATE INDEX ix_worker_overtime_adjustment_worker_fortnight
  ON worker_overtime_adjustment (worker_id, fortnight_start);
CREATE INDEX ix_worker_overtime_adjustment_fortnight ON worker_overtime_adjustment (fortnight_start);
