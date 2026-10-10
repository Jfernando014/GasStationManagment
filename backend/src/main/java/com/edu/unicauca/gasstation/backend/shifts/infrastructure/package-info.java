/**
 * Infrastructure of the shifts module.
 * <ul>
 *   <li>{@code mappers}: domain models and use case results to REST DTOs (MapStruct): catalog and schedule.</li>
 *   <li>{@code persistence}: Spring Data repositories of {@code Role}, {@code ShiftCode} and {@code ShiftAssignment}.
 *       Shift code and assignment queries load the related code and role in the same query ({@code @EntityGraph})
 *       to avoid extra round trips.</li>
 * </ul>
 *
 * <p>Internal to the module: other modules must not use these classes.
 */
package com.edu.unicauca.gasstation.backend.shifts.infrastructure;
