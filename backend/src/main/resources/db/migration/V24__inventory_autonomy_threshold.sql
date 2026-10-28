CREATE TABLE autonomy_threshold
(
    id            BIGSERIAL PRIMARY KEY,
    valid_from    DATE          NOT NULL UNIQUE,
    warning_days  NUMERIC(4, 1) NOT NULL,
    critical_days NUMERIC(4, 1) NOT NULL,
    created_at    TIMESTAMP     NOT NULL,
    CONSTRAINT ck_autonomy_threshold_critical_positive CHECK (critical_days > 0),
    CONSTRAINT ck_autonomy_threshold_order CHECK (critical_days < warning_days)
);