/**
 * Infrastructure of the shifts module.
 * <ul>
 *   <li>{@code mappers}: domain models and use case results to REST DTOs (MapStruct): catalog and schedule.</li>
 *   <li>{@code persistence.entities}: JPA mappings of the {@code role}, {@code shift_code} and
 *       {@code shift_assignment} tables.</li>
 *   <li>{@code persistence.repositories}: Spring Data repositories of those entities ({@code Jpa*Repository}).
 *       Shift code and assignment queries load the related code and role in the same query ({@code @EntityGraph}).</li>
 *   <li>{@code persistence.mappers}: domain models to JPA entities and back.</li>
 *   <li>{@code persistence.adapters}: {@code *RepositoryImpl}, the implementations of the domain repository
 *       interfaces, built on the pieces above.</li>
 * </ul>
 *
 * <p>Internal to the module: other modules must not use these classes.
 */
package com.edu.unicauca.gasstation.backend.shifts.infrastructure;
