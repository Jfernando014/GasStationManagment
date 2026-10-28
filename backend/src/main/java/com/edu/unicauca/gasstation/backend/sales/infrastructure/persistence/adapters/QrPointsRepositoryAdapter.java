package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.AssignedTransaction;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.AssignmentStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.RedebanTransaction;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.QrPointsRepository;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QrPointsRepositoryAdapter implements QrPointsRepository {
    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void replaceRedeban(LocalDate date, List<AssignedTransaction> transactions) {
        jdbc.update("DELETE FROM redeban_qr_transaction WHERE business_date = ?", Date.valueOf(date));
        jdbc.batchUpdate("""
                INSERT INTO redeban_qr_transaction (business_date, tx_time, amount, reference, source_row, worker_id,
                    assignment_status) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, transactions, 500, (ps, t) -> {
            RedebanTransaction tx = t.transaction();
            ps.setDate(1, Date.valueOf(date));
            ps.setTime(2, tx.time() == null ? null : Time.valueOf(tx.time()));
            ps.setBigDecimal(3, tx.amount());
            ps.setObject(4, tx.reference(), Types.VARCHAR);
            ps.setInt(5, tx.row());
            ps.setObject(6, t.workerId(), Types.OTHER);
            ps.setString(7, t.status().name());
        });
    }

    @Override
    public List<AssignedTransaction> findRedeban(LocalDate date) {
        return jdbc.query("SELECT * FROM redeban_qr_transaction WHERE business_date = ? ORDER BY source_row",
                (rs, row) -> {
                    Time time = rs.getTime("tx_time");
                    var tx = new RedebanTransaction(date, time == null ? null : time.toLocalTime(),
                            rs.getBigDecimal("amount"), null, rs.getString("reference"), rs.getInt("source_row"));
                    return new AssignedTransaction(tx, rs.getObject("worker_id", UUID.class),
                            AssignmentStatus.valueOf(rs.getString("assignment_status")));
                }, Date.valueOf(date));
    }

    @Override
    public Optional<PointsDay> findPoints(LocalDate date) {
        return jdbc.query("SELECT points_total, source FROM points_colombia_day WHERE business_date = ?",
                (rs, row) -> new PointsDay(date, rs.getBigDecimal("points_total"), rs.getString("source")),
                Date.valueOf(date)).stream().findFirst();
    }

    @Override
    @Transactional
    public void savePoints(LocalDate date, BigDecimal total, String source) {
        jdbc.update("""
                INSERT INTO points_colombia_day (business_date, points_total, source) VALUES (?, ?, ?)
                ON CONFLICT (business_date) DO UPDATE SET points_total = EXCLUDED.points_total,
                    source = EXCLUDED.source, updated_at = CURRENT_TIMESTAMP
                """, Date.valueOf(date), total, source);
    }
}
