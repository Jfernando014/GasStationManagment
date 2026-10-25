package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftAssignment;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftNovelty;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Fingerprint (SHA-256) of everything the rotation plan depends on: the grid rows it reads, the active titulares and
 * the active novelties of the range. The preview returns it and the confirmation compares it, so a change made from
 * another tab between both steps is detected without keeping any state on the server.
 */
final class ScheduleVersion {

    private ScheduleVersion() {
    }

    /**
     * @param rows      grid rows the plan reads (from twelve days before the first day to the end of the range)
     * @param titulares ids of the active titulares
     * @param novelties active novelties of the range
     */
    static String of(List<ShiftAssignment> rows, Collection<UUID> titulares, List<ShiftNovelty> novelties) {
        StringBuilder text = new StringBuilder();
        rows.stream()
                .sorted(Comparator.comparing(ShiftAssignment::getWorkDate)
                        .thenComparing(row -> row.getWorkerId().toString()))
                .forEach(row -> text.append("row|").append(row.getWorkerId()).append('|').append(row.getWorkDate())
                        .append('|').append(row.getShiftCode().getCode()).append('|').append(row.getOrigin())
                        .append('|').append(Objects.toString(row.getNote(), "")).append('\n'));
        titulares.stream().map(Object::toString).sorted()
                .forEach(id -> text.append("titular|").append(id).append('\n'));
        novelties.stream().map(novelty -> novelty.getId().toString()).sorted()
                .forEach(id -> text.append("novelty|").append(id).append('\n'));
        return HexFormat.of().formatHex(sha256(text.toString()));
    }

    private static byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
