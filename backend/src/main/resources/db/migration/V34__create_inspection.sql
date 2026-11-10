CREATE TABLE inspection
(
    id                 BIGSERIAL PRIMARY KEY,
    code               VARCHAR(20)  NOT NULL,
    category           VARCHAR(20)  NOT NULL,
    target             VARCHAR(150) NOT NULL,
    date_executed      DATE         NOT NULL,
    date_due           DATE         NOT NULL,
    certifier          VARCHAR(150) NOT NULL,
    tolerance_measured VARCHAR(100) NOT NULL,
    cert_doc           VARCHAR(200),
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP    NOT NULL,
    CONSTRAINT uq_inspection_code UNIQUE (code),
    CONSTRAINT ck_inspection_category CHECK (category IN ('ONAC', 'HERMETICIDAD', 'AMBIENTAL', 'BOMBAS')),
    CONSTRAINT ck_inspection_due CHECK (date_due > date_executed)
);

CREATE INDEX idx_inspection_due ON inspection (date_due);
CREATE INDEX idx_inspection_category ON inspection (category);
