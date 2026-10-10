package com.edu.unicauca.gasstation.backend.shifts.domain.models;

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * Catalog entry for a shift code (DIA, DIA6, NOCHE, 12-7...). Mapped to the {@code shift_code} table (migration V6).
 *
 * <p>A shift has up to two segments of hours. Segment 1 may cross midnight (end &lt; start) only for a
 * NIGHT shift with a single segment. The schedule is validated when the object is built and again before
 * it is saved; the table CHECK constraints apply the same rules.
 */
@Entity
@Table(name = "shift_code")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    /** Null for DESCANSO. */
    @Enumerated(EnumType.STRING)
    private ShiftPeriod period;

    @Column(name = "start_hour_1")
    private Short startHour1;

    @Column(name = "end_hour_1")
    private Short endHour1;

    /** Second segment, only for split shifts (6-9y5-9, 6-9y2-6). */
    @Column(name = "start_hour_2")
    private Short startHour2;

    @Column(name = "end_hour_2")
    private Short endHour2;

    /** True for titular codes that need a support worker (DIA6, 9-6). */
    @Column(name = "requires_support", nullable = false)
    private boolean requiresSupport;

    /** Only on support codes: the titular code it pairs with (12-7 -> DIA6). Plain column, no JPA relation. */
    @Column(name = "paired_with_id")
    private UUID pairedWithId;

    @Column(nullable = false)
    private boolean active;

    @Builder
    private ShiftCode(String code, String name, Role role, ShiftPeriod period,
                      Short startHour1, Short endHour1, Short startHour2, Short endHour2,
                      boolean requiresSupport, UUID pairedWithId, boolean active) {
        this.code = code;
        this.name = name;
        this.role = role;
        this.period = period;
        this.startHour1 = startHour1;
        this.endHour1 = endHour1;
        this.startHour2 = startHour2;
        this.endHour2 = endHour2;
        this.requiresSupport = requiresSupport;
        this.pairedWithId = pairedWithId;
        this.active = active;
        validateSchedule();
    }

    /**
     * Working segments: segment 1 and, for split shifts, segment 2. Empty for DESCANSO.
     * The only place where the four hour columns become segments.
     */
    public List<ShiftSegment> segments() {
        List<ShiftSegment> segments = new ArrayList<>(2);
        if (startHour1 != null) {
            segments.add(new ShiftSegment(startHour1, endHour1));
        }
        if (startHour2 != null) {
            segments.add(new ShiftSegment(startHour2, endHour2));
        }
        return List.copyOf(segments);
    }

    /** Sum of the hours of each segment; DESCANSO returns 0. */
    public int totalHours() {
        return segments().stream().mapToInt(ShiftCode::segmentHours).sum();
    }

    /** Hours of one segment, counting past midnight when it ends the next day (NOCHE 18 -> 6 = 12). */
    private static int segmentHours(ShiftSegment segment) {
        int start = segment.startHour();
        int end = segment.endHour();
        return end > start ? end - start : 24 - start + end;
    }

    /**
     * Same rules as the CHECK constraints of the shift_code table:
     * hours in range, start different from end, only a single-segment NIGHT shift crosses midnight,
     * and segment 2 never crosses midnight and starts after segment 1 ends.
     */
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
