package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.calculation.DistributionProfile;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DistributionProfileRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DistributionProfileRepositoryAdapter implements DistributionProfileRepository {
    private final JdbcTemplate jdbc;

    @Override @Transactional
    public StoredProfile register(DistributionProfile profile) {
        var stored = jdbc.queryForObject("""
                INSERT INTO sales_distribution_profiles (name, source, titular_share, support_share, reference_titular_share, reference_support_share)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id, created_at
                """, (rs, row) -> new StoredProfile(rs.getLong("id"), profile, rs.getObject("created_at", OffsetDateTime.class).toInstant()),
                profile.name(), profile.source(), profile.titularShare(), profile.supportShare(), profile.referenceTitularShare(), profile.referenceSupportShare());
        for (int hour = 0; hour < 24; hour++) jdbc.update("INSERT INTO sales_distribution_hour_weights VALUES (?, ?, ?)",
                stored.id(), hour, profile.hourlyWeights().get(hour));
        for (var day : DayOfWeek.values()) jdbc.update("INSERT INTO sales_distribution_day_baselines VALUES (?, ?, ?)",
                stored.id(), day.getValue(), profile.historicalDailyGallons().get(day));
        for (var code : profile.supportShiftCodes()) jdbc.update("INSERT INTO sales_distribution_support_codes VALUES (?, ?)", stored.id(), code);
        return findById(stored.id()).orElseThrow();
    }

    @Override
    public Optional<StoredProfile> findById(long id) {
        var rows = jdbc.query("SELECT * FROM sales_distribution_profiles WHERE id = ?", (rs, row) -> {
            List<BigDecimal> hours = jdbc.query("SELECT weight FROM sales_distribution_hour_weights WHERE profile_id = ? ORDER BY hour",
                    (r, n) -> r.getBigDecimal(1), id);
            var days = new EnumMap<DayOfWeek, BigDecimal>(DayOfWeek.class);
            jdbc.query("SELECT weekday, gallons FROM sales_distribution_day_baselines WHERE profile_id = ?",
                    (org.springframework.jdbc.core.RowCallbackHandler) r -> days.put(DayOfWeek.of(r.getInt(1)), r.getBigDecimal(2)), id);
            var codes = new TreeSet<>(jdbc.query("SELECT shift_code FROM sales_distribution_support_codes WHERE profile_id = ?",
                    (r, n) -> r.getString(1), id));
            var profile = new DistributionProfile(rs.getString("name"), rs.getString("source"), hours, days, codes,
                    rs.getBigDecimal("titular_share"), rs.getBigDecimal("support_share"),
                    rs.getBigDecimal("reference_titular_share"), rs.getBigDecimal("reference_support_share"));
            return new StoredProfile(rs.getLong("id"), profile, rs.getObject("created_at", OffsetDateTime.class).toInstant());
        }, id);
        return rows.stream().findFirst();
    }
}
