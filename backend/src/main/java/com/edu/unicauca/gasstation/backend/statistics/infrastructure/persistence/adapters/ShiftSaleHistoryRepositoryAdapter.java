package com.edu.unicauca.gasstation.backend.statistics.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.statistics.domain.models.ClosedJournalSnapshot;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.JournalSalesProcessing;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistoryRange;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleIdentity;
import com.edu.unicauca.gasstation.backend.statistics.domain.repositories.ShiftSaleHistoryRepository;
import com.edu.unicauca.gasstation.backend.statistics.infrastructure.persistence.mappers.ShiftSaleHistoryRowMapper;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * PostgreSQL adapter with one row lock per operational date. Inserts never replace confirmed snapshots.
 * Explicit transaction templates ensure failure markers commit after the sale transaction has rolled back,
 * including when this port is invoked from a caller whose transaction has already committed or will roll back.
 */
@Repository
public class ShiftSaleHistoryRepositoryAdapter implements ShiftSaleHistoryRepository {

    private static final int MAX_ERROR_LENGTH = 500;
    private static final String SALES_SELECT = """
            SELECT s.*, j.journal_id, j.consecutive, j.closed_at, j.closed_at_nano
            FROM statistics_shift_sale s
            JOIN statistics_journal_sales j ON j.journal_date = s.journal_date
            WHERE j.status = 'PROCESSED'
            """;

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;
    private final TransactionTemplate writeTransaction;

    public ShiftSaleHistoryRepositoryAdapter(JdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbc);
        this.writeTransaction = new TransactionTemplate(transactionManager);
        this.writeTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.writeTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    @Override
    public JournalSalesProcessing saveJournal(ClosedJournalSnapshot journal, List<ShiftSaleHistory> sales) {
        Objects.requireNonNull(journal, "journal");
        try {
            return writeTransaction.execute(transaction -> {
                if (lockJournal(journal)) {
                    return processing(journal.journalDate());
                }
                List<ShiftSaleHistory> snapshots = List.copyOf(sales);
                for (ShiftSaleHistory sale : snapshots) {
                    if (!journal.equals(sale.journal())) {
                        throw new IllegalArgumentException("every sale must describe the same closed journal");
                    }
                    insert(sale);
                }
                jdbc.update("""
                        UPDATE statistics_journal_sales
                        SET journal_id = ?, consecutive = ?, closed_at = ?, closed_at_nano = ?,
                            status = 'PROCESSED', attempts = attempts + 1, sale_count = ?,
                            last_error = NULL, processed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                        WHERE journal_date = ?
                        """, journal.journalId(), journal.consecutive(), seconds(journal), journal.closedAt().getNano(),
                        snapshots.size(), Date.valueOf(journal.journalDate()));
                return processing(journal.journalDate());
            });
        } catch (RuntimeException failure) {
            try {
                markPending(journal, describe(failure));
            } catch (RuntimeException markerFailure) {
                failure.addSuppressed(markerFailure);
            }
            throw failure;
        }
    }

    @Override
    public JournalSalesProcessing markPending(ClosedJournalSnapshot journal, String error) {
        Objects.requireNonNull(journal, "journal");
        Objects.requireNonNull(error, "error");
        if (error.isBlank()) {
            throw new IllegalArgumentException("error must not be blank");
        }
        return writeTransaction.execute(transaction -> {
            if (!lockJournal(journal)) {
                jdbc.update("""
                        UPDATE statistics_journal_sales
                        SET journal_id = ?, consecutive = ?, closed_at = ?, closed_at_nano = ?,
                            attempts = attempts + 1, last_error = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE journal_date = ?
                        """, journal.journalId(), journal.consecutive(), seconds(journal), journal.closedAt().getNano(),
                        abbreviate(error), Date.valueOf(journal.journalDate()));
            }
            return processing(journal.journalDate());
        });
    }

    @Override
    public Optional<JournalSalesProcessing> findProcessing(LocalDate journalDate) {
        Objects.requireNonNull(journalDate, "journalDate");
        return jdbc.query("SELECT * FROM statistics_journal_sales WHERE journal_date = ?",
                ShiftSaleHistoryRowMapper::processing, Date.valueOf(journalDate)).stream().findFirst();
    }

    @Override
    public List<JournalSalesProcessing> findPending() {
        return jdbc.query("SELECT * FROM statistics_journal_sales WHERE status = 'PENDING' ORDER BY journal_date",
                ShiftSaleHistoryRowMapper::processing);
    }

    @Override
    public List<ShiftSaleHistory> findByDate(LocalDate journalDate) {
        Objects.requireNonNull(journalDate, "journalDate");
        return jdbc.query(SALES_SELECT + " AND s.journal_date = ? ORDER BY s.vendor_document, s.shift_name",
                ShiftSaleHistoryRowMapper::sale, Date.valueOf(journalDate));
    }

    @Override
    public Optional<ShiftSaleHistory> findByIdentity(ShiftSaleIdentity identity) {
        Objects.requireNonNull(identity, "identity");
        return jdbc.query(SALES_SELECT + " AND s.journal_date = ? AND s.vendor_document = ? AND s.shift_name = ?",
                ShiftSaleHistoryRowMapper::sale, Date.valueOf(identity.journalDate()),
                identity.vendorDocument(), identity.shiftName()).stream().findFirst();
    }

    @Override
    public ShiftSaleHistoryRange readRange(LocalDate fromInclusive, LocalDate toInclusive) {
        ShiftSaleHistoryRange.validateDates(fromInclusive, toInclusive);
        var processingByDate = new HashMap<LocalDate, JournalSalesProcessing>();
        var salesByDate = new HashMap<LocalDate, List<ShiftSaleHistory>>();
        // A single PostgreSQL statement sees markers and sales at the same MVCC snapshot.
        // The join never exposes unconfirmed sale rows; no per-date queries or additional locks are needed.
        jdbc.query("""
                SELECT j.*, s.*
                FROM statistics_journal_sales j
                LEFT JOIN statistics_shift_sale s
                    ON s.journal_date = j.journal_date AND j.status = 'PROCESSED'
                WHERE j.journal_date BETWEEN ? AND ?
                ORDER BY j.journal_date, s.vendor_document, s.shift_name
                """, (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                    var state = ShiftSaleHistoryRowMapper.processing(rs, 0);
                    var date = state.journal().journalDate();
                    processingByDate.putIfAbsent(date, state);
                    if (rs.getObject("id") != null) {
                        salesByDate.computeIfAbsent(date, ignored -> new ArrayList<>())
                                .add(ShiftSaleHistoryRowMapper.sale(rs, 0));
                    }
                }, Date.valueOf(fromInclusive), Date.valueOf(toInclusive));
        var days = fromInclusive.datesUntil(toInclusive.plusDays(1))
                .map(date -> new ShiftSaleHistoryRange.Day(date, processingByDate.get(date),
                        salesByDate.getOrDefault(date, List.of())))
                .toList();
        return new ShiftSaleHistoryRange(fromInclusive, toInclusive, days);
    }

    /** Insert handles the absent-row race; FOR UPDATE serializes both fresh dates and existing pending dates. */
    private boolean lockJournal(ClosedJournalSnapshot journal) {
        jdbc.update("""
                INSERT INTO statistics_journal_sales
                    (journal_date, journal_id, consecutive, closed_at, closed_at_nano, status, attempts)
                VALUES (?, ?, ?, ?, ?, 'PENDING', 0)
                ON CONFLICT (journal_date) DO NOTHING
                """, Date.valueOf(journal.journalDate()), journal.journalId(), journal.consecutive(),
                seconds(journal), journal.closedAt().getNano());
        String status = jdbc.queryForObject(
                "SELECT status FROM statistics_journal_sales WHERE journal_date = ? FOR UPDATE",
                String.class, Date.valueOf(journal.journalDate()));
        return "PROCESSED".equals(status);
    }

    private JournalSalesProcessing processing(LocalDate date) {
        return findProcessing(date).orElseThrow();
    }

    private void insert(ShiftSaleHistory sale) {
        var parameters = new MapSqlParameterSource()
                .addValue("journal_date", Date.valueOf(sale.identity().journalDate()))
                .addValue("vendor_document", sale.identity().vendorDocument())
                .addValue("shift_name", sale.identity().shiftName())
                .addValue("worker_id", sale.workerId())
                .addValue("vendor_name", sale.vendorName())
                .addValue("worked_code", sale.workedCode())
                .addValue("effective_role", sale.effectiveRole().name())
                .addValue("support", sale.support());
        var values = sale.sales();
        amount(parameters, "motor_gallons", values.motorGallons());
        amount(parameters, "diesel_gallons", values.dieselGallons());
        amount(parameters, "extra_gallons", values.extraGallons());
        amount(parameters, "maxpro_gallons", values.maxproGallons());
        amount(parameters, "basket_dominus_amount_cop", values.basketDominusAmountCop());
        amount(parameters, "dominus_tirilla_cop", values.dominusTirillaCop());
        amount(parameters, "total_delivered_cop", values.totalDeliveredCop());
        namedJdbc.update("""
                INSERT INTO statistics_shift_sale (
                    journal_date,
                    vendor_document,
                    shift_name,
                    worker_id,
                    vendor_name,
                    worked_code,
                    effective_role,
                    support,
                    motor_gallons,
                    motor_gallons_scale,
                    diesel_gallons,
                    diesel_gallons_scale,
                    extra_gallons,
                    extra_gallons_scale,
                    maxpro_gallons,
                    maxpro_gallons_scale,
                    basket_dominus_amount_cop,
                    basket_dominus_amount_cop_scale,
                    dominus_tirilla_cop,
                    dominus_tirilla_cop_scale,
                    total_delivered_cop,
                    total_delivered_cop_scale)
                VALUES (:journal_date, :vendor_document, :shift_name, :worker_id, :vendor_name, :worked_code, :effective_role, :support, :motor_gallons, :motor_gallons_scale, :diesel_gallons, :diesel_gallons_scale, :extra_gallons, :extra_gallons_scale, :maxpro_gallons, :maxpro_gallons_scale, :basket_dominus_amount_cop, :basket_dominus_amount_cop_scale, :dominus_tirilla_cop, :dominus_tirilla_cop_scale, :total_delivered_cop, :total_delivered_cop_scale)
                """, parameters);
    }

    /** Unconstrained PostgreSQL NUMERIC bounds; reject unsupported values before any implicit rounding. */
    private static void amount(MapSqlParameterSource parameters, String name, BigDecimal value) {
        if (value.scale() < -131071 || value.scale() > 16383
                || (long) value.precision() - value.scale() > 131072) {
            throw new IllegalArgumentException(name + " exceeds exact PostgreSQL NUMERIC capacity");
        }
        parameters.addValue(name, value).addValue(name + "_scale", value.scale());
    }

    private static Timestamp seconds(ClosedJournalSnapshot journal) {
        return Timestamp.valueOf(journal.closedAt().withNano(0));
    }

    private static String describe(RuntimeException failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
    }

    private static String abbreviate(String error) {
        return error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
    }
}
