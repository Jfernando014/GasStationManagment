package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import com.edu.unicauca.gasstation.backend.sales.domain.profit.FuelProfitCardSetting;
import java.time.YearMonth;
import java.util.Optional;

public interface FuelProfitCardSettingRepository {

    Optional<FuelProfitCardSetting> findByMonth(YearMonth month);

    /** Inserts or replaces the setting of the month. */
    void save(FuelProfitCardSetting setting);
}
