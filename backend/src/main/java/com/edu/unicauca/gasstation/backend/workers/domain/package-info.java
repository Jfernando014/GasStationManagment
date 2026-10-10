/**
 * Domain of the workers module. Pure Java: no JPA, no DTOs, no Spring Data.
 * <ul>
 *   <li>{@code models}: {@code Worker} and its own rules (trimmed values, starts active, activate, deactivate,
 *       update its details), {@code WorkerChanges} (new values for an edit), {@code WorkerFilter} (optional list
 *       filters) and {@code WorkerDetail} (a worker with its role read from {@code shifts}).</li>
 *   <li>{@code repositories}: interface to store and read workers, implemented by {@code WorkerRepositoryImpl} in
 *       {@code infrastructure.persistence.adapters}.</li>
 *   <li>{@code services}: use cases (create, edit, change status, get, list) and the rules that need the
 *       repository or other modules (unique document, the role exists).</li>
 * </ul>
 *
 * <p>The role is kept as a plain {@code roleId} because roles belong to the {@code shifts} module.
 * Internal to the module: other modules must not use these classes.
 */
package com.edu.unicauca.gasstation.backend.workers.domain;
