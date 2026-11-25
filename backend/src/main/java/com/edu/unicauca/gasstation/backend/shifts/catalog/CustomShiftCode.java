package com.edu.unicauca.gasstation.backend.shifts.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Code of a support shift typed as free hours. The code carries the hours ({@code AP_10-17}, {@code AP_6-9_17-21}), so
 * any module can read them from a stored code without asking today's catalog. These shifts never cross midnight.
 */
public final class CustomShiftCode {

    private static final String PREFIX = "AP";
    private static final Pattern FORMAT = Pattern.compile(PREFIX + "(_\\d{1,2}-\\d{1,2}){1,2}");
    private static final int NIGHT_START_HOUR = 18;
    private static final int NIGHT_END_HOUR = 6;

    private CustomShiftCode() {
    }

    /** Code of those hours: the same hours always give the same code. */
    public static String of(List<ShiftCodeInfo.Segment> segments) {
        return segments.stream()
                .map(segment -> "_" + segment.startHour() + "-" + segment.endHour())
                .collect(Collectors.joining("", PREFIX, ""));
    }

    /** Hours written in the code; empty when it is not the code of a custom shift. */
    public static Optional<List<ShiftCodeInfo.Segment>> segmentsOf(String code) {
        if (code == null || !FORMAT.matcher(code).matches()) {
            return Optional.empty();
        }
        List<ShiftCodeInfo.Segment> segments = new ArrayList<>(2);
        for (String range : code.substring(PREFIX.length() + 1).split("_")) {
            String[] hours = range.split("-");
            int start = Integer.parseInt(hours[0]);
            int end = Integer.parseInt(hours[1]);
            if (end <= start || end > 24) {
                return Optional.empty();
            }
            segments.add(new ShiftCodeInfo.Segment(start, end));
        }
        return Optional.of(List.copyOf(segments));
    }

    /**
     * How a shift code is written for people, in exported files and messages: the hours of a custom shift
     * ({@code 06:00–09:00 y 17:00–24:00}); any other code, and null, as they are.
     */
    public static String displayText(String code) {
        return segmentsOf(code)
                .map(segments -> segments.stream()
                        .map(segment -> "%02d:00–%02d:00".formatted(segment.startHour(), segment.endHour()))
                        .collect(Collectors.joining(" y ")))
                .orElse(code);
    }

    /** A night shift has more than half of its hours between 18:00 and 06:00; with exactly half it is a day shift. */
    public static boolean isNight(List<ShiftCodeInfo.Segment> segments) {
        int total = 0;
        int night = 0;
        for (ShiftCodeInfo.Segment segment : segments) {
            for (int hour = segment.startHour(); hour < segment.endHour(); hour++) {
                total++;
                if (hour >= NIGHT_START_HOUR || hour < NIGHT_END_HOUR) {
                    night++;
                }
            }
        }
        return night * 2 > total;
    }
}
