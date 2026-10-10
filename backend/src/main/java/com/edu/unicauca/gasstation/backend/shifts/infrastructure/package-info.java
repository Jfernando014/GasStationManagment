/**
 * Infrastructure of the shifts module.
 * <ul>
 *   <li>{@code mappers}: domain models to REST DTOs (MapStruct).</li>
 *   <li>{@code persistence.entities}: JPA mappings of the {@code role} and {@code shift_code} tables.</li>
 *   <li>{@code persistence.repositories}: Spring Data repositories of those entities ({@code Jpa*Repository}).
 *       Shift code queries load the role in the same query ({@code @EntityGraph}).</li>
 *   <li>{@code persistence.mappers}: domain models to JPA entities and back.</li>
 *   <li>{@code persistence.adapters}: {@code *RepositoryImpl}, the implementations of the domain repository
 *       interfaces, built on the pieces above.</li>
 * </ul>
 *
 * <p>Internal to the module: other modules must not use these classes.
 */
package com.edu.unicauca.gasstation.backend.shifts.infrastructure;
