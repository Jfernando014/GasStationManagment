-- Customizable UI colors for fuels, shift families, and individual sellers (SR-09).
-- Only customized overrides are stored; omitted keys inherit system defaults.
CREATE TABLE ui_color_setting
(
    id         UUID        PRIMARY KEY,
    category   VARCHAR(20) NOT NULL,
    entity_key VARCHAR(64) NOT NULL,
    color_hex  VARCHAR(7)  NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ui_color_setting_category_key UNIQUE (category, entity_key),
    CONSTRAINT chk_ui_color_setting_category CHECK (category IN ('FUEL', 'SHIFT', 'WORKER')),
    CONSTRAINT chk_ui_color_setting_hex CHECK (color_hex ~ '^#[0-9A-F]{6}$')
);

CREATE INDEX idx_ui_color_setting_category ON ui_color_setting (category);
