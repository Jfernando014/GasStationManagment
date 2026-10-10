/**
 * Infrastructure of the workers module.
 *
 * <p>{@code persistence} holds the Spring Data repository of {@code Worker}, with the queries needed to
 * check that the document is unique and to list workers with optional filters (specifications).
 *
 * <p>Internal to the module: other modules must not use these classes.
 */
package com.edu.unicauca.gasstation.backend.workers.infrastructure;
