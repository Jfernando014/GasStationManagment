package com.edu.unicauca.gasstation.backend.incentives;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.AdditiveSaleUnits;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.IncentiveRule;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.JournalSync;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.SyncStatus;
import com.edu.unicauca.gasstation.backend.incentives.domain.ports.AdditiveCatalogPort;
import com.edu.unicauca.gasstation.backend.incentives.domain.ports.AdditiveInfo;
import com.edu.unicauca.gasstation.backend.incentives.domain.repositories.AdditiveSaleRepository;
import com.edu.unicauca.gasstation.backend.incentives.domain.repositories.IncentiveRuleRepository;
import com.edu.unicauca.gasstation.backend.incentives.domain.repositories.JournalSyncRepository;
import com.edu.unicauca.gasstation.backend.incentives.exception.DuplicateRuleException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Shared builders and in-memory stores for the incentives tests. Nothing here needs Spring or a database. */
public final class IncentiveTestData {

    public static final ZoneId STATION_ZONE = ZoneId.of("America/Bogota");
    /** Same value as {@code incentives.accumulated-from} in application.properties. */
    public static final YearMonth ACCUMULATED_FROM = YearMonth.of(2025, 11);

    public static final Long GASOLINE_ID = 11L;
    public static final Long OCTANE_ID = 12L;
    public static final Long SUPER_ID = 13L;
    public static final Long DIESEL_ID = 14L;

    public static final String GASOLINE_SKU = "RUTA75-GAS";
    public static final String OCTANE_SKU = "RUTA75-OCT";
    public static final String SUPER_SKU = "SUPER-PLUS";
    public static final String DIESEL_SKU = "RUTA75-DSL";

    public static final String GASOLINE_NAME = "Aditivo para gasolina Ruta 75";
    public static final String OCTANE_NAME = "Elevador de octanaje Ruta 75";
    public static final String SUPER_NAME = "Super concentrado plus";
    public static final String DIESEL_NAME = "Aditivo para diésel Ruta 75";

    private IncentiveTestData() {
    }

    /** A clock stopped at the instant, in the station's time zone, like the one the application uses. */
    public static Clock clockAt(String instant) {
        return Clock.fixed(Instant.parse(instant), STATION_ZONE);
    }

    public static AdditiveSaleUnits sale(LocalDate date, String document, String vendorName, String sku,
                                         String productName, int units) {
        return new AdditiveSaleUnits(date, document, vendorName, sku, productName, units);
    }

    public static IncentiveRule rule(Long productId, int valuePerUnit, LocalDate validFrom) {
        return new IncentiveRule(null, productId, valuePerUnit, validFrom, true, null,
                Instant.parse("2025-10-01T00:00:00Z"));
    }

    public static AdditiveInfo additive(Long id, String name, boolean active, int price) {
        return new AdditiveInfo(id, name, active, BigDecimal.valueOf(price));
    }

    /** Sales rows kept by date, read in the order the JPA queries use: date, seller name, SKU. */
    public static final class InMemorySales implements AdditiveSaleRepository {

        private final List<AdditiveSaleUnits> rows = new ArrayList<>();

        public InMemorySales with(AdditiveSaleUnits... sales) {
            rows.addAll(List.of(sales));
            return this;
        }

        public InMemorySales with(Collection<AdditiveSaleUnits> sales) {
            rows.addAll(sales);
            return this;
        }

        @Override
        public void replaceDay(LocalDate journalDate, List<AdditiveSaleUnits> sales) {
            rows.removeIf(row -> row.journalDate().equals(journalDate));
            rows.addAll(sales);
        }

        @Override
        public List<AdditiveSaleUnits> findByDate(LocalDate journalDate) {
            return findBetween(journalDate, journalDate);
        }

        @Override
        public List<AdditiveSaleUnits> findBetween(LocalDate from, LocalDate to) {
            return rows.stream()
                    .filter(row -> !row.journalDate().isBefore(from) && !row.journalDate().isAfter(to))
                    .sorted(Comparator.comparing(AdditiveSaleUnits::journalDate)
                            .thenComparing(AdditiveSaleUnits::vendorName)
                            .thenComparing(AdditiveSaleUnits::sku))
                    .toList();
        }

        @Override
        public List<AdditiveSaleUnits> findBetweenForVendor(LocalDate from, LocalDate to, String vendorDocument) {
            return findBetween(from, to).stream()
                    .filter(row -> row.vendorDocument().equals(vendorDocument))
                    .toList();
        }
    }

    /** Rules by additive. Like the unique index, it refuses two rules of an additive with the same validity. */
    public static final class InMemoryRules implements IncentiveRuleRepository {

        private final List<IncentiveRule> rules = new ArrayList<>();
        private long nextId = 1;

        public InMemoryRules with(IncentiveRule... newRules) {
            for (IncentiveRule rule : newRules) {
                save(rule);
            }
            return this;
        }

        public InMemoryRules with(Collection<IncentiveRule> newRules) {
            newRules.forEach(this::save);
            return this;
        }

        @Override
        public IncentiveRule save(IncentiveRule rule) {
            boolean duplicate = rules.stream().anyMatch(existing ->
                    existing.productId().equals(rule.productId()) && existing.validFrom().equals(rule.validFrom()));
            if (duplicate) {
                throw new DuplicateRuleException();
            }
            IncentiveRule stored = new IncentiveRule(nextId++, rule.productId(), rule.valuePerUnit(),
                    rule.validFrom(), rule.active(), rule.reason(), rule.createdAt());
            rules.add(stored);
            return stored;
        }

        @Override
        public List<IncentiveRule> findHistory(Long productId) {
            return rules.stream().filter(rule -> rule.productId().equals(productId)).toList();
        }

        @Override
        public List<IncentiveRule> findByProductIds(Collection<Long> productIds) {
            return rules.stream().filter(rule -> productIds.contains(rule.productId())).toList();
        }

        @Override
        public List<IncentiveRule> findAll() {
            return List.copyOf(rules);
        }

        public List<IncentiveRule> all() {
            return List.copyOf(rules);
        }
    }

    /** The additive catalog, found by id or by code (the SKU sales reports). */
    public static final class InMemoryCatalog implements AdditiveCatalogPort {

        private final Map<Long, AdditiveInfo> byId = new TreeMap<>();
        private final Map<String, Long> idByCode = new HashMap<>();

        public InMemoryCatalog with(String code, AdditiveInfo additive) {
            byId.put(additive.id(), additive);
            idByCode.put(code, additive.id());
            return this;
        }

        @Override
        public Optional<AdditiveInfo> findById(Long productId) {
            return Optional.ofNullable(byId.get(productId));
        }

        @Override
        public Optional<AdditiveInfo> findByCode(String code) {
            return Optional.ofNullable(code).map(String::strip).map(idByCode::get).map(byId::get);
        }

        @Override
        public List<AdditiveInfo> listActiveAdditives() {
            return byId.values().stream()
                    .filter(AdditiveInfo::active)
                    .sorted(Comparator.comparing(AdditiveInfo::name))
                    .toList();
        }
    }

    /** The processing state of each journal date. */
    public static final class InMemorySync implements JournalSyncRepository {

        private final Map<LocalDate, JournalSync> states = new TreeMap<>();

        public InMemorySync with(JournalSync sync) {
            states.put(sync.journalDate(), sync);
            return this;
        }

        @Override
        public JournalSync save(JournalSync sync) {
            states.put(sync.journalDate(), sync);
            return sync;
        }

        @Override
        public Optional<JournalSync> findByDate(LocalDate journalDate) {
            return Optional.ofNullable(states.get(journalDate));
        }

        @Override
        public Optional<LocalDate> findLastDate() {
            return states.keySet().stream().max(Comparator.naturalOrder());
        }

        @Override
        public List<JournalSync> findPending() {
            return states.values().stream().filter(sync -> sync.status() == SyncStatus.PENDING).toList();
        }

        public int size() {
            return states.size();
        }
    }
}
