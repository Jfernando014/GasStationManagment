/**
 * Infrastructure of the workers module.
 * <ul>
 *   <li>{@code mappers}: REST DTOs to domain models and back (MapStruct).</li>
 *   <li>{@code persistence}: Spring Data repository of {@code Worker} and the specifications that build the
 *       list with optional filters.</li>
 * </ul>
 *
 * <p>Internal to the module: other modules must not use these classes.
 */
package com.edu.unicauca.gasstation.backend.workers.infrastructure;
