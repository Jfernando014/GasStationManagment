-- Support shifts typed by the administrator as free hours are stored as catalog rows marked as custom, one per
-- schedule, so the grid, the novelties and the worked hours keep working with a shift code. Every existing code is a
-- standard one. A custom schedule is never paired with a titular code.

ALTER TABLE shift_code
  ADD COLUMN custom BOOLEAN NOT NULL DEFAULT FALSE
    CONSTRAINT ck_shift_code_custom CHECK (NOT custom OR (paired_with_id IS NULL AND NOT requires_support));
