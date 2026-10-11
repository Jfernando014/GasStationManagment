ALTER TABLE tank
    ADD COLUMN gauge_table_id BIGINT,
    ADD CONSTRAINT fk_tank_gauge_table FOREIGN KEY (gauge_table_id) REFERENCES gauge_table (id);