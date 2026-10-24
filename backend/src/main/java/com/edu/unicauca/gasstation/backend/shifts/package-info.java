/**
 * Shifts module: catalog of shift codes and roles (S1-22), Colombian holidays, the monthly shift schedule (S1-27)
 * and the staff of each operational day (S2-20).
 *
 * <p>Public API (usable by other modules): {@link com.edu.unicauca.gasstation.backend.shifts.ShiftExternalService}
 * and the records {@link com.edu.unicauca.gasstation.backend.shifts.RoleInfo} and
 * {@link com.edu.unicauca.gasstation.backend.shifts.ShiftCodeInfo}. Every subpackage is internal.
 *
 * <p>{@link com.edu.unicauca.gasstation.backend.shifts.DayStaffQuery} answers who works a day, with which code, in
 * which role and on which dispenser. Its result types are public too:
 * {@link com.edu.unicauca.gasstation.backend.shifts.DayStaff},
 * {@link com.edu.unicauca.gasstation.backend.shifts.DayStaffMember},
 * {@link com.edu.unicauca.gasstation.backend.shifts.DayStaffNovelty},
 * {@link com.edu.unicauca.gasstation.backend.shifts.DayStaffAbsentee},
 * {@link com.edu.unicauca.gasstation.backend.shifts.NoveltyType},
 * {@link com.edu.unicauca.gasstation.backend.shifts.Step1Status} and
 * {@link com.edu.unicauca.gasstation.backend.shifts.DayStaffWarning}.
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
