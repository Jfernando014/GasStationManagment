/**
 * Shifts module: catalog of shift codes and roles (S1-22), Colombian holidays and the monthly shift schedule (S1-27).
 *
 * <p>Public API (usable by other modules): {@link com.edu.unicauca.gasstation.backend.shifts.ShiftExternalService}
 * and the records {@link com.edu.unicauca.gasstation.backend.shifts.RoleInfo} and
 * {@link com.edu.unicauca.gasstation.backend.shifts.ShiftCodeInfo}. Every subpackage is internal.
 *
 * <p>{@link com.edu.unicauca.gasstation.backend.shifts.ScheduleWorkerDirectory} and its record
 * {@link com.edu.unicauca.gasstation.backend.shifts.ScheduleWorkerInfo} go the other way: this module declares them
 * and {@code workers} implements them, so the schedule can read workers without {@code shifts} depending on
 * {@code workers}.
 *
 * <p>Layers: {@code api} (REST controllers and DTOs) and {@code infrastructure} (persistence and mappers) depend
 * on {@code domain} (pure models, repository interfaces and use cases); {@code domain} depends on neither.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Shifts")
package com.edu.unicauca.gasstation.backend.shifts;
