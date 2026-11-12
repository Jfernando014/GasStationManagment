package com.edu.unicauca.gasstation.backend.workers.domain.ports;

import com.edu.unicauca.gasstation.backend.workers.domain.models.DayHoursReading;
import java.time.LocalDate;

/** Who worked an operational day and for how many hours, read from the shifts module. */
public interface DayStaffPort {

    /**
     * One record per worker who worked and per worker who was absent, with 0 hours. A day without schedule gives no
     * records. Inactive workers are left out, as the shifts module does. The date is the operational date, used as
     * the shifts module delivers it.
     */
    DayHoursReading read(LocalDate workDate);
}
