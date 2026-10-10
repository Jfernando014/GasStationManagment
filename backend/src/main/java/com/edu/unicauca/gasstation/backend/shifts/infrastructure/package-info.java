/**
 * Infrastructure of the shifts module.
 * <ul>
 *   <li>{@code mappers}: domain models to REST DTOs (MapStruct).</li>
 *   <li>{@code persistence}: Spring Data repositories of {@code Role} and {@code ShiftCode}. Shift code queries
 *       load the role in the same query ({@code @EntityGraph}) to avoid extra round trips.</li>
 * </ul>
 *
 * <p>Internal to the module: other modules must not use these classes.
 */
package com.edu.unicauca.gasstation.backend.shifts.infrastructure;
