CREATE TABLE tank
(
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code                 VARCHAR(10)    NOT NULL,
    name                 VARCHAR(80)    NOT NULL,
    fuel_type            VARCHAR(20)    NOT NULL,
    max_capacity_gallons NUMERIC(10, 2) NOT NULL,
    max_height_cm        NUMERIC(6, 2)  NOT NULL,
    tolerance_cm         NUMERIC(5, 2)  NOT NULL,
    CONSTRAINT uq_tank_code UNIQUE (code),
    CONSTRAINT ck_tank_fuel_type
        CHECK (fuel_type IN ('MOTOR', 'DIESEL', 'EXTRA', 'MAX_PRO')),
    CONSTRAINT ck_tank_capacity_positive CHECK (max_capacity_gallons > 0),
    CONSTRAINT ck_tank_max_height_positive CHECK (max_height_cm > 0),
    CONSTRAINT ck_tank_tolerance_non_negative CHECK (tolerance_cm >= 0)
);