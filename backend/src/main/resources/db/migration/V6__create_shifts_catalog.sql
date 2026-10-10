-- S1-22 · Shift catalog (shifts module).
-- Creates the role and shift_code tables. Their rows (2 roles, 11 shift codes) are loaded by ShiftDataInitializer.

CREATE TABLE role (
  id        UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  name      VARCHAR(255) NOT NULL CONSTRAINT uk_role_name UNIQUE,      -- TITULAR, APOYO
  dispenser INTEGER      NOT NULL CONSTRAINT ck_role_dispenser CHECK (dispenser > 0)  -- TITULAR = 1, APOYO = 3
);

CREATE TABLE shift_code (
  id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  code             VARCHAR(255) NOT NULL CONSTRAINT uk_shift_code_code UNIQUE,  -- DIA, DIA6, NOCHE, 12-7...
  name             VARCHAR(255) NOT NULL,
  role_id          UUID         CONSTRAINT fk_shift_code_role REFERENCES role (id),  -- NULL only for DESCANSO
  period           VARCHAR(255) CONSTRAINT ck_shift_code_period CHECK (period IN ('DAY', 'NIGHT')),
  start_hour_1     SMALLINT,
  end_hour_1       SMALLINT,
  start_hour_2     SMALLINT,    -- second segment, only for split shifts (6-9y5-9, 6-9y2-6)
  end_hour_2       SMALLINT,
  requires_support BOOLEAN      NOT NULL DEFAULT FALSE,
  paired_with_id   UUID         CONSTRAINT fk_shift_code_paired_with REFERENCES shift_code (id),  -- support code -> titular code
  active           BOOLEAN      NOT NULL DEFAULT TRUE,

  -- Segment 1: both hours or none (DESCANSO). Start 0-23, end 1-24, start <> end.
  CONSTRAINT ck_shift_code_segment_1 CHECK (
    (start_hour_1 IS NULL AND end_hour_1 IS NULL)
    OR (start_hour_1 IS NOT NULL AND end_hour_1 IS NOT NULL
        AND start_hour_1 BETWEEN 0 AND 23
        AND end_hour_1 BETWEEN 1 AND 24
        AND start_hour_1 <> end_hour_1)
  ),

  -- end < start means the shift ends the next day: allowed only for a NIGHT shift with a single segment.
  CONSTRAINT ck_shift_code_overnight CHECK (
    end_hour_1 IS NULL
    OR end_hour_1 > start_hour_1
    OR (period IS NOT NULL AND period = 'NIGHT' AND start_hour_2 IS NULL AND end_hour_2 IS NULL)
  ),

  -- Segment 2: both hours or none. Never crosses midnight and starts after segment 1 ends.
  CONSTRAINT ck_shift_code_segment_2 CHECK (
    (start_hour_2 IS NULL AND end_hour_2 IS NULL)
    OR (start_hour_2 IS NOT NULL AND end_hour_2 IS NOT NULL AND end_hour_1 IS NOT NULL
        AND start_hour_2 BETWEEN 0 AND 23
        AND end_hour_2 BETWEEN 1 AND 24
        AND start_hour_2 < end_hour_2
        AND start_hour_2 >= end_hour_1)
  )
);
