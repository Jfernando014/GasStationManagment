/**
 * Shifts module: catalog of shift codes and roles, Colombian holidays, the monthly shift schedule with its automatic
 * rotation, step 1 of the journal with its novelties, and the staff of each operational day.
 *
 * <p>Other modules may only use its three named interfaces; every other subpackage is internal:
 * <ul>
 *   <li>{@code catalog}: the read-only catalog of shift codes and roles
 *       ({@link com.edu.unicauca.gasstation.backend.shifts.catalog.ShiftExternalService}).</li>
 *   <li>{@code daystaff}: who works an operational day, with which code, in which role and on which dispenser
 *       ({@link com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaffQuery} and
 *       {@link com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaff}).</li>
 *   <li>{@code spi}: what other modules implement for this one, so it never depends on them. {@code workers}
 *       implements {@link com.edu.unicauca.gasstation.backend.shifts.spi.ScheduleWorkerDirectory} and {@code sales}
 *       implements {@link com.edu.unicauca.gasstation.backend.shifts.spi.DayJournalPort}; until {@code sales} does,
 *       a temporary in-memory implementation of {@code infrastructure.journal} is used.</li>
 * </ul>
 *
 * <p>Layers: {@code api} (REST controllers and DTOs) and {@code infrastructure} (persistence, mappers, the catalog
 * seed and the temporary journal) depend on {@code domain} (pure models, repository interfaces and use cases);
 * {@code domain} depends on neither. Errors are {@code exception.ShiftException} with a {@code exception.ShiftError}.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Shifts")
package com.edu.unicauca.gasstation.backend.shifts;
