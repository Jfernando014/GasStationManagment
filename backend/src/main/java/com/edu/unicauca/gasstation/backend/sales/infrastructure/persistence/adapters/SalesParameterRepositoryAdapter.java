package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterKey;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterValue;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.SalesParameterRepository;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesParameterRepositoryAdapter implements SalesParameterRepository {
    private final JdbcTemplate jdbc;

    @Override
    public Optional<SalesParameterValue> findEffective(SalesParameterKey key, LocalDate date) {
        return jdbc.query("""
                SELECT numeric_value, valid_from FROM sales_parameter
                WHERE parameter_key = ? AND valid_from <= ?
                ORDER BY valid_from DESC LIMIT 1
                """, (rs, row) -> new SalesParameterValue(key, rs.getBigDecimal("numeric_value"),
                rs.getDate("valid_from").toLocalDate()), key.name(), Date.valueOf(date)).stream().findFirst();
    }

    @Override
    public List<SalesParameterValue> history(SalesParameterKey key) {
        return jdbc.query("""
                SELECT numeric_value, valid_from FROM sales_parameter
                WHERE parameter_key = ? ORDER BY valid_from DESC
                """, (rs, row) -> new SalesParameterValue(key, rs.getBigDecimal("numeric_value"),
                rs.getDate("valid_from").toLocalDate()), key.name());
    }

    @Override
    @Transactional
    public SalesParameterValue register(SalesParameterValue value) {
        jdbc.update("""
                INSERT INTO sales_parameter (parameter_key, numeric_value, valid_from) VALUES (?, ?, ?)
                ON CONFLICT (parameter_key, valid_from) DO UPDATE SET numeric_value = EXCLUDED.numeric_value
                """, value.key().name(), value.value(), Date.valueOf(value.validFrom()));
        return value;
    }
}
