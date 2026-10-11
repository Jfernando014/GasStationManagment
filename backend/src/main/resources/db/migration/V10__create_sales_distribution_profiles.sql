-- SCRUM-40: append-only, explicitly sourced calculation parameter snapshots.
-- No sales history or shift assignments are copied into these tables.
CREATE TABLE sales_distribution_profiles (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL CHECK (length(trim(name)) > 0),
    source VARCHAR(500) NOT NULL CHECK (length(trim(source)) > 0),
    titular_share NUMERIC(13,12) NOT NULL,
    support_share NUMERIC(13,12) NOT NULL,
    reference_titular_share NUMERIC(13,12) NOT NULL,
    reference_support_share NUMERIC(13,12) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_sales_distribution_shares CHECK (
        titular_share BETWEEN 0 AND 1 AND support_share BETWEEN 0 AND 1
        AND titular_share + support_share = 1
        AND reference_titular_share BETWEEN 0 AND 1 AND reference_support_share BETWEEN 0 AND 1
        AND reference_titular_share + reference_support_share = 1
    )
);
CREATE TABLE sales_distribution_hour_weights (
    profile_id BIGINT NOT NULL REFERENCES sales_distribution_profiles(id),
    hour SMALLINT NOT NULL CHECK (hour BETWEEN 0 AND 23),
    weight NUMERIC(30,12) NOT NULL CHECK (weight >= 0 AND weight <> 'NaN'::numeric),
    PRIMARY KEY (profile_id, hour)
);
CREATE TABLE sales_distribution_day_baselines (
    profile_id BIGINT NOT NULL REFERENCES sales_distribution_profiles(id),
    weekday SMALLINT NOT NULL CHECK (weekday BETWEEN 1 AND 7),
    gallons NUMERIC(30,12) NOT NULL CHECK (gallons >= 0 AND gallons <> 'NaN'::numeric),
    PRIMARY KEY (profile_id, weekday)
);
CREATE TABLE sales_distribution_support_codes (
    profile_id BIGINT NOT NULL REFERENCES sales_distribution_profiles(id),
    shift_code VARCHAR(30) NOT NULL CHECK (length(trim(shift_code)) > 0),
    PRIMARY KEY (profile_id, shift_code)
);
