/**
 * What other modules implement for the shifts module: {@link
 * com.edu.unicauca.gasstation.backend.shifts.spi.ScheduleWorkerDirectory} (the workers that the schedule reads, with
 * {@link com.edu.unicauca.gasstation.backend.shifts.spi.ScheduleWorkerInfo}) and
 * {@link com.edu.unicauca.gasstation.backend.shifts.spi.DayJournalPort} (the journal of a date). They are declared here
 * so this module never depends on the modules that implement them.
 */
@org.springframework.modulith.NamedInterface("spi")
package com.edu.unicauca.gasstation.backend.shifts.spi;
