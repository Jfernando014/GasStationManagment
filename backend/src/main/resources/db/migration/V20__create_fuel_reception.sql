CREATE TABLE fuel_reception
(
    id             BIGSERIAL PRIMARY KEY,
    tank_id        BIGINT        NOT NULL REFERENCES tank (id),
    reception_date DATE          NOT NULL,
    gallons        NUMERIC(8, 2) NOT NULL,
    supplier       VARCHAR(100)  NOT NULL,
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL,
    CONSTRAINT ck_fuel_reception_gallons CHECK (gallons > 0)
);

CREATE INDEX idx_fuel_reception_tank_date ON fuel_reception (tank_id, reception_date);
CREATE INDEX idx_fuel_reception_date ON fuel_reception (reception_date);