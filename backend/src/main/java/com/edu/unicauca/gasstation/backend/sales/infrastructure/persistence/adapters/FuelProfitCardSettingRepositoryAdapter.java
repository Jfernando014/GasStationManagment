package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.profit.CardPercentageMode;
import com.edu.unicauca.gasstation.backend.sales.domain.profit.FuelProfitCardSetting;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.FuelProfitCardSettingRepository;
import java.sql.Date;
import java.time.YearMonth;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FuelProfitCardSettingRepositoryAdapter implements FuelProfitCardSettingRepository {
    private final JdbcTemplate jdbc;

    @Override
    public Optional<FuelProfitCardSetting> findByMonth(YearMonth month) {
        return jdbc.query("""
                SELECT mode, manual_percentage FROM fuel_profit_card_setting WHERE month_start = ?
                """, (rs, row) -> new FuelProfitCardSetting(month.toString(),
                CardPercentageMode.valueOf(rs.getString("mode")), rs.getBigDecimal("manual_percentage")),
                Date.valueOf(month.atDay(1))).stream().findFirst();
    }

    @Override
    @Transactional
    public void save(FuelProfitCardSetting setting) {
        jdbc.update("""
                INSERT INTO fuel_profit_card_setting (month_start, mode, manual_percentage) VALUES (?, ?, ?)
                ON CONFLICT (month_start) DO UPDATE SET mode = EXCLUDED.mode,
                    manual_percentage = EXCLUDED.manual_percentage, updated_at = CURRENT_TIMESTAMP
                """, Date.valueOf(YearMonth.parse(setting.month()).atDay(1)), setting.mode().name(),
                setting.manualPercentage());
    }
}
