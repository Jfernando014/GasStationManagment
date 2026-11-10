package com.edu.unicauca.gasstation.backend.statistics.infrastructure.persistence.mappers;

import com.edu.unicauca.gasstation.backend.statistics.domain.models.ClosedJournalSnapshot;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.JournalSalesProcessing;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleAmounts;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleIdentity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/** JDBC mapping of immutable domain values; scale metadata also preserves scientific-notation scales. */
public final class ShiftSaleHistoryRowMapper {

    private ShiftSaleHistoryRowMapper() {
    }

    public static JournalSalesProcessing processing(ResultSet rs, int row) throws SQLException {
        var processedAt = rs.getObject("processed_at", OffsetDateTime.class);
        return new JournalSalesProcessing(journal(rs),
                JournalSalesProcessing.Status.valueOf(rs.getString("status")), rs.getInt("attempts"),
                rs.getObject("sale_count", Integer.class), rs.getString("last_error"),
                processedAt == null ? null : processedAt.toInstant(),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant());
    }

    public static ShiftSaleHistory sale(ResultSet rs, int row) throws SQLException {
        var snapshot = journal(rs);
        return new ShiftSaleHistory(snapshot,
                new ShiftSaleIdentity(snapshot.journalDate(), rs.getString("vendor_document"), rs.getString("shift_name")),
                rs.getObject("worker_id", UUID.class), rs.getString("vendor_name"), rs.getString("worked_code"),
                ShiftSaleHistory.EffectiveRole.valueOf(rs.getString("effective_role")),
                new ShiftSaleAmounts(
                        decimal(rs, "motor_gallons"), decimal(rs, "diesel_gallons"),
                        decimal(rs, "extra_gallons"), decimal(rs, "maxpro_gallons"),
                        decimal(rs, "basket_dominus_amount_cop"), decimal(rs, "dominus_tirilla_cop"),
                        decimal(rs, "total_delivered_cop")));
    }

    private static ClosedJournalSnapshot journal(ResultSet rs) throws SQLException {
        return new ClosedJournalSnapshot(rs.getObject("journal_id", Long.class),
                rs.getDate("journal_date").toLocalDate(), rs.getLong("consecutive"),
                rs.getObject("closed_at", LocalDateTime.class).withNano(rs.getInt("closed_at_nano")));
    }

    private static BigDecimal decimal(ResultSet rs, String column) throws SQLException {
        return rs.getBigDecimal(column).setScale(rs.getInt(column + "_scale"), RoundingMode.UNNECESSARY);
    }
}
