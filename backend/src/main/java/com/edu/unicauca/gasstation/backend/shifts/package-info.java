/**
 * Shifts module: catalog of shift codes and roles (S1-22).
 *
 * <p>Public API (usable by other modules): {@link com.edu.unicauca.gasstation.backend.shifts.ShiftExternalService}
 * and the records {@link com.edu.unicauca.gasstation.backend.shifts.RoleInfo} and
 * {@link com.edu.unicauca.gasstation.backend.shifts.ShiftCodeInfo}. Every subpackage is internal.
 *
 * <p>Layers: {@code api} (REST controller and DTOs) → {@code domain} (entities and use cases) →
 * {@code infrastructure} (Spring Data repositories and DTO mappers).
 */
@org.springframework.modulith.ApplicationModule(displayName = "Shifts")
package com.edu.unicauca.gasstation.backend.shifts;
