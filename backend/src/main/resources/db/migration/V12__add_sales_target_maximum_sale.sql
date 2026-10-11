-- Configuration only: no formula, cap on sales or relationship with target_value is inferred.
-- Existing rows remain NULL because their maximum sale is unknown.
ALTER TABLE sales_targets ADD COLUMN maximum_sale NUMERIC(30,12);
ALTER TABLE sales_targets ADD CONSTRAINT ck_sales_targets_maximum_sale
    CHECK (maximum_sale IS NULL OR (maximum_sale >= 0 AND maximum_sale <> 'NaN'::numeric));
