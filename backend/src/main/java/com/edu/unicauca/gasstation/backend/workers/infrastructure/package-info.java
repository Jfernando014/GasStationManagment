/**
 * Infrastructure of the workers module.
 * <ul>
 *   <li>{@code mappers}: REST DTOs to domain models and back (MapStruct).</li>
 *   <li>{@code persistence.entities}: JPA mapping of the {@code worker} table, of the hours kept per day with their
 *       segments, of their processing state, and of the overtime parameters and adjustments.</li>
 *   <li>{@code persistence.repositories}: Spring Data repositories of those entities ({@code Jpa*Repository}).</li>
 *   <li>{@code persistence.mappers}: worker domain model to JPA entity and back.</li>
 *   <li>{@code persistence.adapters}: {@code *RepositoryImpl}, the implementations of the domain repository
 *       interfaces; they build the list filters ({@code WorkerSpecifications}) and translate a unique constraint
 *       rejected by the database into a domain exception.</li>
 *   <li>{@code listeners}: the listener of closed journals that keeps the worked hours.</li>
 *   <li>{@code shifts}: everything that talks to the shifts module through its public API: the readers of the
 *       day staff and of the public holidays, and {@code WorkerScheduleDirectory}, which lets the shift schedule
 *       read workers.</li>
 * </ul>
 *
 * <p>Internal to the module: other modules must not use these classes.
 */
package com.edu.unicauca.gasstation.backend.workers.infrastructure;
