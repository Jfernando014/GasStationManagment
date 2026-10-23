CREATE TABLE competitor (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    location VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_competitor_name CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_competitor_brand CHECK (length(trim(brand)) > 0),
    CONSTRAINT ck_competitor_location CHECK (length(trim(location)) > 0)
);

CREATE TABLE competitor_price_observation (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    competitor_id BIGINT NOT NULL,
    fuel_type VARCHAR(20) NOT NULL,
    price_per_gallon NUMERIC(12,2) NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_competitor_observation FOREIGN KEY (competitor_id) REFERENCES competitor(id),
    CONSTRAINT ck_competitor_observation_fuel CHECK (fuel_type IN ('MOTOR', 'DIESEL', 'EXTRA', 'MAX_PRO')),
    CONSTRAINT ck_competitor_observation_price CHECK (price_per_gallon > 0 AND price_per_gallon <> 'NaN'::numeric),
    CONSTRAINT ck_competitor_observation_dates CHECK (
        isfinite(observed_at) AND isfinite(created_at) AND observed_at <= created_at
    )
);

-- ID breaks ties when two manually recorded observations have the same observation time.
CREATE INDEX ix_competitor_observation_latest
    ON competitor_price_observation (competitor_id, fuel_type, observed_at DESC, id DESC);
CREATE INDEX ix_competitor_observation_history
    ON competitor_price_observation (observed_at DESC, id DESC);
