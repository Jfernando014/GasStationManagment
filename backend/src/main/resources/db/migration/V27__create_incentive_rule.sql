-- Incentive rules: pesos per unit sold of an additive from valid_from. Rules are never edited or deleted.

CREATE TABLE incentive_rule (
  id             BIGSERIAL    PRIMARY KEY,
  -- Product of the inventory module; no foreign key because modules do not share tables.
  product_id     BIGINT       NOT NULL,
  value_per_unit INTEGER      NOT NULL
    CONSTRAINT ck_incentive_rule_value CHECK (value_per_unit BETWEEN 0 AND 100000),
  valid_from     DATE         NOT NULL,
  active         BOOLEAN      NOT NULL DEFAULT TRUE,
  reason         VARCHAR(255),
  created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_incentive_rule_product_valid_from ON incentive_rule (product_id, valid_from);
CREATE INDEX ix_incentive_rule_product_valid_from_desc ON incentive_rule (product_id, valid_from DESC);
