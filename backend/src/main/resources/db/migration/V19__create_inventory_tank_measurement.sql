CREATE TABLE tank_measurement
(
    id           BIGSERIAL PRIMARY KEY,
    journal_date DATE      NOT NULL,
    measured_at  TIMESTAMP NOT NULL,
    CONSTRAINT uq_tank_measurement_journal_date UNIQUE (journal_date)
);

CREATE TABLE tank_reading
(
    id             BIGSERIAL PRIMARY KEY,
    measurement_id BIGINT        NOT NULL REFERENCES tank_measurement (id) ON DELETE CASCADE,
    tank_id        BIGINT        NOT NULL REFERENCES tank (id),
    height_cm      NUMERIC(5, 1) NOT NULL,
    volume_gallons NUMERIC(8, 2) NOT NULL,
    CONSTRAINT uq_tank_reading_measurement_tank UNIQUE (measurement_id, tank_id),
    CONSTRAINT ck_tank_reading_height CHECK (height_cm >= 0),
    CONSTRAINT ck_tank_reading_volume CHECK (volume_gallons >= 0)
);

CREATE INDEX idx_tank_reading_tank_id ON tank_reading (tank_id);