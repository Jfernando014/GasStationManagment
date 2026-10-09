/**
 * Domain of the shifts module: the JPA entities of the shift catalog.
 * <ul>
 *   <li>{@link com.edu.unicauca.gasstation.backend.shifts.domain.Role}: TITULAR or APOYO, with the dispenser
 *       each role works on.</li>
 *   <li>{@link com.edu.unicauca.gasstation.backend.shifts.domain.ShiftCode}: each of the 11 shift codes, with
 *       up to two time segments, its role, the pairing rule between support and titular codes, and the
 *       validation of its schedule (only a NIGHT shift may end after midnight).</li>
 *   <li>{@link com.edu.unicauca.gasstation.backend.shifts.domain.ShiftPeriod}: DAY or NIGHT.</li>
 * </ul>
 *
 * <p>Internal to the module: other modules read this data through
 * {@link com.edu.unicauca.gasstation.backend.shifts.ShiftExternalService}, never through these entities.
 */
package com.edu.unicauca.gasstation.backend.shifts.domain;
