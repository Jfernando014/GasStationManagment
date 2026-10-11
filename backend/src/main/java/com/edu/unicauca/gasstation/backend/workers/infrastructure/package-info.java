/**
 * Infrastructure of the workers module.
 * <ul>
 *   <li>{@code mappers}: REST DTOs to domain models and back (MapStruct).</li>
 *   <li>{@code persistence.entities}: JPA mapping of the {@code worker} table.</li>
 *   <li>{@code persistence.repositories}: Spring Data repository of that entity ({@code JpaWorkerRepository}).</li>
 *   <li>{@code persistence.mappers}: domain model to JPA entity and back.</li>
 *   <li>{@code persistence.adapters}: {@code WorkerRepositoryImpl}, the implementation of the domain repository
 *       interface; builds the list filters ({@code WorkerSpecifications}) and translates a duplicate document
 *       rejected by the database into a domain exception.</li>
 * </ul>
 *
 * <p>Internal to the module: other modules must not use these classes.
 */
package com.edu.unicauca.gasstation.backend.workers.infrastructure;
