/**
 * Domain of the workers module: the {@link com.edu.unicauca.gasstation.backend.workers.domain.Worker} entity
 * and the rules that belong to it (starts active, activate, deactivate, update its details).
 *
 * <p>Does not know repositories, services or other modules. The role is kept as a plain {@code roleId}
 * because the {@code role} table belongs to the {@code shifts} module.
 *
 * <p>Internal to the module: other modules must not use these classes.
 */
package com.edu.unicauca.gasstation.backend.workers.domain;
