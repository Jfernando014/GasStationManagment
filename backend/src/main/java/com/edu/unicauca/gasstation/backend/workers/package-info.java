/**
 * Workers module: management of the station sellers, the hours each one worked on every closed day, and the
 * fortnight overtime sheet worked out from them.
 *
 * <p>Depends on {@code shifts} only through its public API: {@link com.edu.unicauca.gasstation.backend.shifts.catalog.ShiftExternalService}
 * to validate roles and read their name and dispenser, {@link com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaffQuery}
 * to read who worked a day, {@link com.edu.unicauca.gasstation.backend.shifts.holidays.HolidayQuery} to read the
 * public holidays, and {@link com.edu.unicauca.gasstation.backend.shifts.spi.ScheduleWorkerDirectory},
 * which this module implements so the shift schedule can read workers. {@code shifts} does not depend on this module.
 * It also listens to {@link com.edu.unicauca.gasstation.backend.sales.JournalClosedEvent}, the public event of
 * {@code sales}, which does not depend on this module either.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Workers")
package com.edu.unicauca.gasstation.backend.workers;
