/**
 * Shifts module: catalog of shift codes and roles, Colombian holidays, the monthly shift schedule, step 1 of the
 * journal with its novelties and the staff of each operational day.
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
 * <p>{@link com.edu.unicauca.gasstation.backend.shifts.DayJournalPort} goes the other way too: this module declares
 * it and {@code sales} implements it, so step 1 can ask whether a journal is editable and mark its step 1 without
 * {@code shifts} depending on {@code sales}. Until {@code sales} publishes it, a temporary in-memory implementation
 * of {@code infrastructure.journal} is used.
 *
 * <p>Layers: {@code api} (REST controllers and DTOs) and {@code infrastructure} (persistence and mappers) depend
 * on {@code domain} (pure models, repository interfaces and use cases); {@code domain} depends on neither.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Shifts")
package com.edu.unicauca.gasstation.backend.shifts;
