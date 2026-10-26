package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.DayFranchiseTotals;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.PeriodTotals;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.VendorDeclaration;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.CardReconciliationRepository;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardReconciliationRepositoryAdapter implements CardReconciliationRepository {
    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void upsert(LocalDate date, VendorDeclaration d) {
        jdbc.update("""
                INSERT INTO credibanco_vendor_day (business_date, worker_id, shift_code, mastercard, visa, amex,
                    mastercard_dawn, visa_dawn, amex_dawn, planilla_credibanco, planilla_qr, planilla_redemptions)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (business_date, worker_id) DO UPDATE SET
                    shift_code = EXCLUDED.shift_code, mastercard = EXCLUDED.mastercard, visa = EXCLUDED.visa,
                    amex = EXCLUDED.amex, mastercard_dawn = EXCLUDED.mastercard_dawn, visa_dawn = EXCLUDED.visa_dawn,
                    amex_dawn = EXCLUDED.amex_dawn, planilla_credibanco = EXCLUDED.planilla_credibanco,
                    planilla_qr = EXCLUDED.planilla_qr, planilla_redemptions = EXCLUDED.planilla_redemptions,
                    updated_at = CURRENT_TIMESTAMP
                """, ps -> {
            ps.setDate(1, Date.valueOf(date));
            ps.setObject(2, d.workerId());
            ps.setObject(3, d.shiftCode(), Types.VARCHAR);
            ps.setBigDecimal(4, d.mastercard());
            ps.setBigDecimal(5, d.visa());
            ps.setBigDecimal(6, d.amex());
            ps.setBigDecimal(7, d.mastercardDawn());
            ps.setBigDecimal(8, d.visaDawn());
            ps.setBigDecimal(9, d.amexDawn());
            ps.setBigDecimal(10, d.planillaCredibanco());
            ps.setBigDecimal(11, d.planillaQr());
            ps.setBigDecimal(12, d.planillaRedemptions());
        });
    }

    @Override
    @Transactional
    public boolean delete(LocalDate date, UUID workerId) {
        return jdbc.update("DELETE FROM credibanco_vendor_day WHERE business_date = ? AND worker_id = ?",
                Date.valueOf(date), workerId) > 0;
    }

    @Override
    public List<VendorDeclaration> findByDate(LocalDate date) {
        return jdbc.query("SELECT * FROM credibanco_vendor_day WHERE business_date = ? ORDER BY id",
                (rs, row) -> new VendorDeclaration(rs.getObject("worker_id", UUID.class), rs.getString("shift_code"),
                        rs.getBigDecimal("mastercard"), rs.getBigDecimal("visa"), rs.getBigDecimal("amex"),
                        rs.getBigDecimal("mastercard_dawn"), rs.getBigDecimal("visa_dawn"),
                        rs.getBigDecimal("amex_dawn"), rs.getBigDecimal("planilla_credibanco"),
                        rs.getBigDecimal("planilla_qr"), rs.getBigDecimal("planilla_redemptions")),
                Date.valueOf(date));
    }

    @Override
    public Map<LocalDate, DayFranchiseTotals> findDailyTotals(LocalDate from, LocalDate to) {
        Map<LocalDate, DayFranchiseTotals> result = new TreeMap<>();
        if (from.isAfter(to)) {
            return result;
        }
        jdbc.query("""
                SELECT business_date, SUM(amex) amex, SUM(mastercard) mastercard, SUM(visa) visa,
                       SUM(amex_dawn) amex_dawn, SUM(mastercard_dawn) mastercard_dawn, SUM(visa_dawn) visa_dawn,
                       SUM(planilla_qr) qr
                FROM credibanco_vendor_day WHERE business_date BETWEEN ? AND ?
                GROUP BY business_date
                """, (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
            result.put(rs.getDate("business_date").toLocalDate(), new DayFranchiseTotals(
                    rs.getBigDecimal("amex"), rs.getBigDecimal("mastercard"), rs.getBigDecimal("visa"),
                    rs.getBigDecimal("amex_dawn"), rs.getBigDecimal("mastercard_dawn"),
                    rs.getBigDecimal("visa_dawn"), rs.getBigDecimal("qr")));
        }, Date.valueOf(from), Date.valueOf(to));
        return result;
    }

    @Override
    public Optional<PeriodTotals> findPreviousMonthBaseline(YearMonth month) {
        return jdbc.query("SELECT * FROM credibanco_previous_month WHERE month_start = ?",
                (rs, row) -> PeriodTotals.of(rs.getBigDecimal("mastercard"), rs.getBigDecimal("visa"),
                        rs.getBigDecimal("amex"), rs.getBigDecimal("qr"), "BASELINE"),
                Date.valueOf(month.atDay(1))).stream().findFirst();
    }

    @Override
    @Transactional
    public void saveBaseline(YearMonth month, PeriodTotals totals) {
        jdbc.update("""
                INSERT INTO credibanco_previous_month (month_start, mastercard, visa, amex, qr) VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (month_start) DO UPDATE SET mastercard = EXCLUDED.mastercard, visa = EXCLUDED.visa,
                    amex = EXCLUDED.amex, qr = EXCLUDED.qr, updated_at = CURRENT_TIMESTAMP
                """, Date.valueOf(month.atDay(1)), totals.mastercard(), totals.visa(), totals.amex(), totals.qr());
    }
}
