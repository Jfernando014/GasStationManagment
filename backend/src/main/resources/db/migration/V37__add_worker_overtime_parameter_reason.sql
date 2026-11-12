-- Optional reason of a version of the overtime parameters. Versions created before this column have none.

ALTER TABLE worker_overtime_parameter
  ADD COLUMN reason VARCHAR(255)
    CONSTRAINT ck_worker_overtime_parameter_reason CHECK (reason IS NULL OR length(btrim(reason)) BETWEEN 3 AND 255);
