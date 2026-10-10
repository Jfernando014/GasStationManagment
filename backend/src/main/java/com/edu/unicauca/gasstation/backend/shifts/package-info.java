/**
 * Shifts module: catalog of shift codes and roles (S1-22).
 *
 * <p>Public API (usable by other modules): {@link com.edu.unicauca.gasstation.backend.shifts.ShiftExternalService}
 * and the records {@link com.edu.unicauca.gasstation.backend.shifts.RoleInfo} and
 * {@link com.edu.unicauca.gasstation.backend.shifts.ShiftCodeInfo}. Every subpackage is internal.
 *
 * <p>Layers: {@code api} (REST controllers and DTOs) and {@code infrastructure} (persistence and mappers) depend
 * on {@code domain} (pure models, repository interfaces and use cases); {@code domain} depends on neither.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Shifts")
package com.edu.unicauca.gasstation.backend.shifts;
