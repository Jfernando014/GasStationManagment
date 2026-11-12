/**
 * Domain of the workers module. Pure Java: no JPA, no DTOs, no Spring Data.
 * <ul>
 *   <li>{@code models}: {@code Worker} and its own rules (trimmed values, starts active, activate, deactivate,
 *       update its details), {@code WorkerChanges} (new values for an edit), {@code WorkerFilter} (optional list
 *       filters) and {@code WorkerDetail} (a worker with its role read from {@code shifts}). Also the hours kept per
 *       closed day ({@code WorkerDayHours}, {@code HoursSync}) and the overtime sheet ({@code OvertimeSheet},
 *       {@code OvertimeParameters}, {@code OvertimeAdjustment}, {@code Fortnight}).</li>
 *   <li>{@code ports}: what the domain reads from other modules, implemented in {@code infrastructure.shifts}: the
 *       day staff ({@code DayStaffPort}) and the public holidays ({@code HolidayCalendarPort}).</li>
 *   <li>{@code repositories}: interfaces to store and read workers, the hours kept per day and their processing
 *       state, the overtime parameters and the manual adjustments, implemented in
 *       {@code infrastructure.persistence.adapters}.</li>
 *   <li>{@code services}: use cases. {@code WorkerService} (create, edit, change status, get, list; with the rules
 *       that need the repository or other modules: unique document, the role exists),
 *       {@code WorkerHoursRegistrationService} and {@code WorkerHoursService} (keep and read the hours of closed
 *       days), and the overtime ones: {@code OvertimeCalculator} (the classification rules, pure),
 *       {@code OvertimeSheetService}, {@code OvertimeParameterService} and {@code OvertimeAdjustmentService}.</li>
 * </ul>
 *
 * <p>The role is kept as a plain {@code roleId} because roles belong to the {@code shifts} module.
 * Internal to the module: other modules must not use these classes.
 */
package com.edu.unicauca.gasstation.backend.workers.domain;
