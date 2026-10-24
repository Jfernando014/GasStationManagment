package com.edu.unicauca.gasstation.backend.sales;

import com.edu.unicauca.gasstation.backend.GasStationApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class SalesStatisticsModularityTest {
    @Test void applicationRespectsModuleBoundaries() {
        ApplicationModules.of(GasStationApplication.class).verify();
    }
}
