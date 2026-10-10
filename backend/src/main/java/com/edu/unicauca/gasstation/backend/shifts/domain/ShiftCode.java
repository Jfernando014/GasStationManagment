package com.edu.unicauca.gasstation.backend.shifts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/**
 * Catalog entry for a shift code (DIA, DIA6, NOCHE, 12-7...).
 * A shift has up to two segments; segment 1 may cross midnight (end < start) only for a NIGHT shift.
 */
@Entity
@Table(name = "shift_code")
@Getter
@Setter
@NoArgsConstructor
public class ShiftCode {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    /** Null for DESCANSO. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @Enumerated(EnumType.STRING)
    private ShiftPeriod period;

    @Column(name = "start_hour_1")
    private Short startHour1;

    @Column(name = "end_hour_1")
    private Short endHour1;

    @Column(name = "start_hour_2")
    private Short startHour2;

    @Column(name = "end_hour_2")
    private Short endHour2;

    @Column(name = "requires_support", nullable = false)
    private boolean requiresSupport;

    /** Only on support codes: the titular code it pairs with (12-7 -> DIA6). */
    @Column(name = "paired_with_id")
    private UUID pairedWithId;

    @Column(nullable = false)
    private boolean active;

    /** Sum of the hours of each segment; DESCANSO returns 0. */
    public int totalHours() {
        return segmentHours(startHour1, endHour1) + segmentHours(startHour2, endHour2);
    }

    private static int segmentHours(Short start, Short end) {
        if (start == null || end == null) {
            return 0;
        }
        return end > start ? end - start : 24 - start + end;
    }

    @PrePersist
    @PreUpdate
    void validateSchedule() {
        if (startHour1 == null || endHour1 == null) {
            if (startHour1 != null || endHour1 != null || startHour2 != null || endHour2 != null) {
                throw invalid("segment 1 must have both hours, or the shift must have no segments");
            }
            return;
        }
        validateBounds(startHour1, endHour1);
        boolean crossesMidnight = endHour1 < startHour1;
        if (crossesMidnight && period != ShiftPeriod.NIGHT) {
            throw invalid("only a NIGHT shift may end after midnight");
        }

        if (startHour2 == null && endHour2 == null) {
            return;
        }
        if (startHour2 == null || endHour2 == null) {
            throw invalid("segment 2 must have both hours");
        }
        if (crossesMidnight) {
            throw invalid("a shift that crosses midnight cannot have a second segment");
        }
        validateBounds(startHour2, endHour2);
        if (endHour2 < startHour2) {
            throw invalid("segment 2 cannot cross midnight");
        }
        if (startHour2 < endHour1) {
            throw invalid("segment 2 must start after segment 1 ends");
        }
    }

    private void validateBounds(short start, short end) {
        if (start < 0 || start > 23 || end < 1 || end > 24 || start == end) {
            throw invalid("start must be 0-23, end 1-24 and different from start");
        }
    }

    private IllegalStateException invalid(String reason) {
        return new IllegalStateException("Invalid schedule for shift code " + code + ": " + reason);
    }
}
