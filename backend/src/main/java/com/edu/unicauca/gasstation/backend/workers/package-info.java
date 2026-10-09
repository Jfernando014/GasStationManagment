/**
 * Workers module: management of the station sellers (HU-9).
 *
 * <p>Depends on {@code shifts} only through {@link com.edu.unicauca.gasstation.backend.shifts.ShiftExternalService}
 * to validate roles and read their name and dispenser. {@code shifts} does not depend on this module.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Workers")
package com.edu.unicauca.gasstation.backend.workers;
