package com.edu.unicauca.gasstation.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Lets {@code @ApplicationModuleListener} run independently of the thread that closes the journal. A listener failure
 * is recorded as an incomplete publication and does not roll back an already committed journal close.
 */
@Configuration
@EnableAsync
public class AsyncEventsConfiguration {
}
