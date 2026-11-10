package com.edu.unicauca.gasstation.backend.incentives.domain.models;

import java.time.YearMonth;

/** First month counted in the accumulated total, which never restarts after it. Comes from configuration. */
public record AccumulationStart(YearMonth month) {
}
