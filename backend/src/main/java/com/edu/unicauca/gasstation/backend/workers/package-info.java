/**
 * Workers module: management of the station sellers (HU-9).
 *
 * <p>Depends on {@code shifts} only through its public API: {@link com.edu.unicauca.gasstation.backend.shifts.ShiftExternalService}
 * to validate roles and read their name and dispenser, and {@link com.edu.unicauca.gasstation.backend.shifts.ScheduleWorkerDirectory},
 * which this module implements so the shift schedule can read workers. {@code shifts} does not depend on this module.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Workers")
package com.edu.unicauca.gasstation.backend.workers;
