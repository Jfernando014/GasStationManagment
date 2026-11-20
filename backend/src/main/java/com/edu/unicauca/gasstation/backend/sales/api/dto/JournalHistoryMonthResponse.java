package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.JournalCalendarDay;
import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.JournalHistoryCalendar;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Calendario mensual del historial de jornadas, con los nombres que usa el frontend")
public record JournalHistoryMonthResponse(
        String yearMonth,
        int closedSystemCount,
        int migratedCount,
        int withMarkersCount,
        List<DayItem> days
) {

    @Schema(description = "Un día del mes. source es JOURNAL, MIGRATED o EMPTY; state es CLOSED, OPEN, MIGRATED o PENDING; "
            + "markers solo trae DISCREPANCY o ALERT")
    public record DayItem(
            LocalDate date,
            int dayOfMonth,
            String source,
            String state,
            Long journalId,
            Long consecutive,
            List<String> markers
    ) {}

    public static JournalHistoryMonthResponse from(JournalHistoryCalendar calendar) {
        return new JournalHistoryMonthResponse(
                calendar.yearMonth(),
                calendar.closedInSystemCount(),
                calendar.migratedDaysCount(),
                calendar.daysWithIssuesCount(),
                calendar.days().stream().map(JournalHistoryMonthResponse::toDay).toList()
        );
    }

    private static DayItem toDay(JournalCalendarDay day) {
        return new DayItem(
                day.date(),
                day.dayOfMonth(),
                day.type(),
                state(day.type(), day.status()),
                day.journalId(),
                day.consecutive(),
                markers(day.markers())
        );
    }

    /** JOURNAL con CLOSED es CLOSED, JOURNAL con otro estado es OPEN, MIGRATED es MIGRATED y EMPTY es PENDING. */
    static String state(String type, String status) {
        return switch (type) {
            case "JOURNAL" -> "CLOSED".equals(status) ? "CLOSED" : "OPEN";
            case "MIGRATED" -> "MIGRATED";
            default -> "PENDING";
        };
    }

    static List<String> markers(List<String> markers) {
        return markers == null ? List.of() : markers.stream().map(JournalHistoryMonthResponse::marker).toList();
    }

    private static String marker(String marker) {
        return switch (marker) {
            case "descuadre" -> "DISCREPANCY";
            case "alerta" -> "ALERT";
            default -> marker;
        };
    }
}
