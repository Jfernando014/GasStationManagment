package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterKey;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterValue;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.SalesParameterRepository;
import com.edu.unicauca.gasstation.backend.sales.exception.SalesParameterNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Configuration owned by Ventas (daily savings, Credibanco commission) with effective dates. */
@Service
@RequiredArgsConstructor
public class SalesParameterService {
    private final SalesParameterRepository repository;

    @Transactional(readOnly = true)
    public SalesParameterValue effective(SalesParameterKey key, LocalDate date) {
        return repository.findEffective(key, date).orElseThrow(() -> new SalesParameterNotFoundException(
                "No hay un valor de '" + key.slug() + "' vigente el " + date));
    }

    @Transactional(readOnly = true)
    public List<SalesParameterValue> history(SalesParameterKey key) {
        return repository.history(key);
    }

    @Transactional
    public SalesParameterValue register(SalesParameterKey key, BigDecimal value, LocalDate validFrom) {
        if (validFrom == null) {
            throw new IllegalArgumentException("La fecha de vigencia es obligatoria");
        }
        key.validate(value);
        return repository.register(new SalesParameterValue(key, value, validFrom));
    }
}
