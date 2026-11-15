-- Audit of database export and restore operations. This table is excluded from the dumps.
CREATE TABLE IF NOT EXISTS backup_audit (
  id          BIGSERIAL PRIMARY KEY,
  username    VARCHAR(120) NOT NULL,
  action      VARCHAR(10) NOT NULL,
  file_name   VARCHAR(255),
  size_bytes  BIGINT NOT NULL DEFAULT 0,
  status      VARCHAR(10) NOT NULL,
  message     VARCHAR(500),
  client_ip   VARCHAR(64),
  started_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_backup_audit_started_at ON backup_audit (started_at DESC);
