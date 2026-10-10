/**
 * Domain of the shifts module. Pure Java: no JPA, no DTOs, no Spring Data.
 * <ul>
 *   <li>{@code models}: {@code Role} (TITULAR or APOYO with its dispenser), {@code ShiftCode} (each of the 11 codes
 *       with up to two segments, its role, the pairing rule and the validation of its schedule),
 *       {@code ShiftSegment}, {@code ShiftPeriod} (DAY or NIGHT) and {@code ShiftAssignment} (the shift code of a
 *       worker on one day). Also the results of the schedule use cases: {@code ScheduleChange},
 *       {@code ScheduleView}, {@code WorkerShifts} and {@code DailyCoverage}.</li>
 *   <li>{@code repositories}: interfaces the domain needs to store and read data, implemented by the
 *       {@code *RepositoryImpl} classes in {@code infrastructure.persistence.adapters}.</li>
 *   <li>{@code services}: use cases. {@code ShiftCatalogService} (catalog; also implements the public
 *       {@code ShiftExternalService}), {@code HolidayService} (Colombian holidays) and {@code ShiftScheduleService}
 *       (monthly schedule and its daily coverage).</li>
 * </ul>
 *
 * <p>Internal to the module: other modules read this data through
 * {@link com.edu.unicauca.gasstation.backend.shifts.ShiftExternalService}, never through these classes.
 */
package com.edu.unicauca.gasstation.backend.shifts.domain;
