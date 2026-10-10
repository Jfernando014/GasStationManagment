-- S1-25 · Station sellers (workers module).
-- Workers are never deleted, only deactivated. Dispenser and products are derived from the role, not stored here.

CREATE TABLE worker (
  id        UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  full_name VARCHAR(150) NOT NULL,
  -- Identity document (cédula). Text: it may have leading zeros.
  -- The constraint name is used by WorkersExceptionHandler to answer 409; do not rename it.
  document  VARCHAR(20)  NOT NULL CONSTRAINT uk_worker_document UNIQUE,
  -- role belongs to the shifts module. The entity keeps role_id as a plain UUID, so this FK only exists here.
  role_id   UUID         NOT NULL CONSTRAINT fk_worker_role REFERENCES role (id),
  active    BOOLEAN      NOT NULL DEFAULT TRUE
);
