package com.edu.unicauca.gasstation.backend.competition;

import com.edu.unicauca.gasstation.backend.GasStationApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class CompetitionModuleTest {
    @Test void respectsModuleBoundariesAndHasNoDependencyCycles() {
        ApplicationModules.of(GasStationApplication.class).verify();
    }
}
