-- Rules of the monthly fuel variation alerts, with validity date. The oldest row covers every month.
CREATE TABLE variation_alert_setting
(
    id                          BIGSERIAL     PRIMARY KEY,
    valid_from                  DATE          NOT NULL UNIQUE,
    cumulative_limit_percentage NUMERIC(7, 4) NOT NULL,
    escalation_days             INTEGER       NOT NULL,
    shortage_limit_1_to_49      INTEGER       NOT NULL,
    shortage_limit_50_to_74     INTEGER       NOT NULL,
    shortage_limit_75_to_124    INTEGER       NOT NULL,
    shortage_limit_125_to_249   INTEGER       NOT NULL,
    shortage_limit_250_to_499   INTEGER       NOT NULL,
    shortage_limit_500_or_more  INTEGER       NOT NULL,
    created_at                  TIMESTAMP     NOT NULL,
    CONSTRAINT ck_variation_alert_setting_percentage
        CHECK (cumulative_limit_percentage > 0 AND cumulative_limit_percentage <= 100),
    CONSTRAINT ck_variation_alert_setting_days CHECK (escalation_days BETWEEN 1 AND 10),
    -- a range of larger shortages never allows more occurrences than a range of smaller ones
    CONSTRAINT ck_variation_alert_setting_limits CHECK (
        shortage_limit_500_or_more >= 1
        AND shortage_limit_250_to_499 >= shortage_limit_500_or_more
        AND shortage_limit_125_to_249 >= shortage_limit_250_to_499
        AND shortage_limit_75_to_124 >= shortage_limit_125_to_249
        AND shortage_limit_50_to_74 >= shortage_limit_75_to_124
        AND shortage_limit_1_to_49 >= shortage_limit_50_to_74)
);

INSERT INTO variation_alert_setting (valid_from, cumulative_limit_percentage, escalation_days,
    shortage_limit_1_to_49, shortage_limit_50_to_74, shortage_limit_75_to_124, shortage_limit_125_to_249,
    shortage_limit_250_to_499, shortage_limit_500_or_more, created_at)
VALUES (DATE '2000-01-01', 0.5, 3, 12, 8, 6, 4, 2, 1, CURRENT_TIMESTAMP);
