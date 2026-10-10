/**
 * Domain of the shifts module.
 * <ul>
 *   <li>{@code models}: JPA entities of the catalog with their own rules. {@code Role} (TITULAR or APOYO with
 *       its dispenser), {@code ShiftCode} (each of the 11 codes with up to two segments, its role, the pairing
 *       rule and the validation of its schedule) and {@code ShiftPeriod} (DAY or NIGHT).</li>
 *   <li>{@code services}: use cases of the catalog. {@code ShiftCatalogService} also implements the public
 *       {@code ShiftExternalService}.</li>
 * </ul>
 *
 * <p>Internal to the module: other modules read this data through
 * {@link com.edu.unicauca.gasstation.backend.shifts.ShiftExternalService}, never through these classes.
 */
package com.edu.unicauca.gasstation.backend.shifts.domain;
