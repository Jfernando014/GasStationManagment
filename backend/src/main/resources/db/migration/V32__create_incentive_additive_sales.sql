-- Additive units sold per seller on each closed journal, and the processing state of each journal date.
-- The units feed the incentive calculation that runs later.

CREATE TABLE incentive_additive_sale (
  id              BIGSERIAL    PRIMARY KEY,
  journal_date    DATE         NOT NULL,
  -- Seller document and product SKU exactly as sales reports them; no foreign keys because modules do not share
  -- tables. The ids of the worker and of the product are resolved later, when incentives are calculated.
  vendor_document VARCHAR(30)  NOT NULL,
  vendor_name     VARCHAR(150) NOT NULL,
  sku             VARCHAR(50)  NOT NULL,
  product_name    VARCHAR(150) NOT NULL,
  units           INTEGER      NOT NULL
    CONSTRAINT ck_incentive_additive_sale_units CHECK (units > 0),
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_incentive_additive_sale_day_vendor_sku
  ON incentive_additive_sale (journal_date, vendor_document, sku);
CREATE INDEX ix_incentive_additive_sale_sku_date ON incentive_additive_sale (sku, journal_date);

CREATE TABLE incentive_journal_sync (
  journal_date DATE         PRIMARY KEY,
  -- Data of the closed journal as sales reported it; no foreign key because modules do not share tables. They are
  -- empty when the date was processed by hand and no closing was announced.
  journal_id   BIGINT,
  consecutive  BIGINT,
  closed_at    TIMESTAMP,
  status       VARCHAR(12)  NOT NULL
    CONSTRAINT ck_incentive_journal_sync_status CHECK (status IN ('PROCESSED', 'PENDING')),
  attempts     INTEGER      NOT NULL DEFAULT 0,
  last_error   VARCHAR(500),
  processed_at TIMESTAMPTZ,
  updated_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
