CREATE TABLE gauge_table
(
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    CONSTRAINT uq_gauge_table_name UNIQUE (name)
);

CREATE TABLE gauge_point
(
    id             BIGSERIAL PRIMARY KEY,
    gauge_table_id BIGINT        NOT NULL,
    height_cm      NUMERIC(5, 1) NOT NULL,
    volume_gallons NUMERIC(8, 2) NOT NULL,
    CONSTRAINT fk_gauge_point_gauge_table FOREIGN KEY (gauge_table_id)
        REFERENCES gauge_table (id) ON DELETE CASCADE,
    CONSTRAINT uq_gauge_point_table_height UNIQUE (gauge_table_id, height_cm),
    CONSTRAINT ck_gauge_point_height CHECK (height_cm >= 0),
    CONSTRAINT ck_gauge_point_volume CHECK (volume_gallons >= 0)
);