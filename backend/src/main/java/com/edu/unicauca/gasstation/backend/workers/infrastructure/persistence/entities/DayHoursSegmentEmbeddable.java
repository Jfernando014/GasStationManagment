package com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One row of {@code worker_day_hours_segment}: a block of hours of a day record. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DayHoursSegmentEmbeddable {

    @Column(name = "start_hour", nullable = false)
    private int startHour;

    @Column(name = "end_hour", nullable = false)
    private int endHour;

    @Column(nullable = false)
    private int hours;
}
