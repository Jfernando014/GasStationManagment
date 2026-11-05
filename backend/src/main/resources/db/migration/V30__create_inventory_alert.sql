CREATE TABLE inventory_alert
(
    id            BIGSERIAL PRIMARY KEY,
    alert_type    VARCHAR(20)   NOT NULL,
    fuel_type     VARCHAR(20)   NOT NULL,
    level         VARCHAR(10)   NOT NULL,
    status        VARCHAR(10)   NOT NULL,
    autonomy_days NUMERIC(8, 2) NOT NULL,
    opened_on     DATE          NOT NULL,
    created_at    TIMESTAMP     NOT NULL,
    read_at       TIMESTAMP,
    attended_at   TIMESTAMP,
    attend_note   VARCHAR(200),
    resolved_at   TIMESTAMP,
    CONSTRAINT ck_inventory_alert_type CHECK (alert_type IN ('AUTONOMY')),
    CONSTRAINT ck_inventory_alert_level CHECK (level IN ('WARNING', 'CRITICAL')),
    CONSTRAINT ck_inventory_alert_status CHECK (status IN ('ACTIVE', 'READ', 'ATTENDED', 'RESOLVED')),
    CONSTRAINT ck_inventory_alert_days CHECK (autonomy_days >= 0),
    CONSTRAINT ck_inventory_alert_attended CHECK (status <> 'ATTENDED' OR attended_at IS NOT NULL),
    CONSTRAINT ck_inventory_alert_resolved CHECK ((status = 'RESOLVED') = (resolved_at IS NOT NULL))
);

-- At most one open alert per type and fuel
CREATE UNIQUE INDEX ux_inventory_alert_open
    ON inventory_alert (alert_type, fuel_type) WHERE status <> 'RESOLVED';

CREATE INDEX ix_inventory_alert_opened_on ON inventory_alert (opened_on);
CREATE INDEX ix_inventory_alert_status ON inventory_alert (status);