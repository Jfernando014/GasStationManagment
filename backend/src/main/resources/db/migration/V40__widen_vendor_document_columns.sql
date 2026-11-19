-- Widen vendor_document from VARCHAR(30) to VARCHAR(64) so both worker UUIDs (36 chars)
-- and Colombian identity documents / legacy placeholders fit cleanly without overflow.

ALTER TABLE vendor_shift_closes
    ALTER COLUMN vendor_document TYPE VARCHAR(64);

ALTER TABLE incentive_additive_sale
    ALTER COLUMN vendor_document TYPE VARCHAR(64);
