/**
 * Visual identity shared by every Excel file the application generates. It is open on purpose:
 * the sales and reports modules use it without depending on each other.
 *
 * <p>Spring Modulith 1.1 has no {@code type} attribute. The default value of
 * {@code allowedDependencies} already allows every dependency, which is the same as an open
 * module; the annotation is kept to give the module a name in the documentation.</p>
 */
@org.springframework.modulith.ApplicationModule(displayName = "Excel")
package com.edu.unicauca.gasstation.backend.excel;
