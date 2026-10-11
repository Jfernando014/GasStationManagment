package com.edu.unicauca.gasstation.backend.workers.domain.models;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Getter;

/**
 * Seller of the station.
 *
 * <p>Rules that belong to the worker itself:
 * <ul>
 *   <li>Name and document are saved without leading or trailing spaces.</li>
 *   <li>The name is saved normalized: single spaces and the first letter of each word in upper case, the rest
 *       in lower case; connectors ({@code de}, {@code del}, {@code la}...) stay in lower case except as the first
 *       word ("María del Pilar de la Torre").</li>
 *   <li>A new worker starts active.</li>
 *   <li>It is never deleted; it is deactivated so its sales, closings and shifts history is kept.</li>
 *   <li>It can be edited even when inactive.</li>
 * </ul>
 * The role is referenced by id: roles belong to the {@code shifts} module. The dispenser and the products are
 * derived from the role and are never stored here.
 */
@Getter
public class Worker {

    private static final Locale SPANISH = Locale.forLanguageTag("es");

    /** Words that stay in lower case inside a name, except as the first word. */
    private static final Set<String> NAME_CONNECTORS = Set.of("de", "del", "la", "las", "los", "y", "e");

    /** Null until the worker is saved. */
    private final UUID id;

    private String fullName;

    /** Identity document (cédula). Text, not a number: it may have leading zeros. */
    private String document;

    private UUID roleId;

    private boolean active;

    /**
     * Creates a new worker, always active.
     */
    public Worker(String fullName, String document, UUID roleId) {
        this.id = null;
        this.active = true;
        updateDetails(fullName, document, roleId);
    }

    private Worker(UUID id, String fullName, String document, UUID roleId, boolean active) {
        this.id = Objects.requireNonNull(id, "id");
        this.fullName = Objects.requireNonNull(fullName, "fullName");
        this.document = Objects.requireNonNull(document, "document");
        this.roleId = Objects.requireNonNull(roleId, "roleId");
        this.active = active;
    }

    /**
     * Rebuilds a worker that already exists (for example, read from the database), keeping its data as stored.
     */
    public static Worker restore(UUID id, String fullName, String document, UUID roleId, boolean active) {
        return new Worker(id, fullName, document, roleId, active);
    }

    /** Replaces the editable data: the name is normalized and the document loses leading and trailing spaces. */
    public void updateDetails(String fullName, String document, UUID roleId) {
        this.fullName = normalizeName(Objects.requireNonNull(fullName, "fullName"));
        this.document = Objects.requireNonNull(document, "document").strip();
        this.roleId = Objects.requireNonNull(roleId, "roleId");
    }

    /** Marks the worker as active. Does nothing if it already is. */
    public void activate() {
        this.active = true;
    }

    /** Marks the worker as inactive. Does nothing if it already is. */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Single spaces, first letter of each word in upper case and the rest in lower case. Connectors stay in lower
     * case except as the first word, and each part of a hyphenated word is capitalized ("Torre-Gómez").
     */
    private static String normalizeName(String name) {
        String[] words = name.strip().toLowerCase(SPANISH).split("\s+");
        return IntStream.range(0, words.length)
                .mapToObj(i -> i > 0 && NAME_CONNECTORS.contains(words[i]) ? words[i] : capitalizeParts(words[i]))
                .collect(Collectors.joining(" "));
    }

    private static String capitalizeParts(String word) {
        return Arrays.stream(word.split("-", -1))
                .map(part -> part.isEmpty() ? part : part.substring(0, 1).toUpperCase(SPANISH) + part.substring(1))
                .collect(Collectors.joining("-"));
    }
}
