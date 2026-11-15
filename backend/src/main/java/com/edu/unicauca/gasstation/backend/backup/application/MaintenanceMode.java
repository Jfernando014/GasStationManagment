package com.edu.unicauca.gasstation.backend.backup.application;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/**
 * Keeps the application in maintenance while a restore is running.
 *
 * @author javiersolanop777
 */
@Component
public class MaintenanceMode {

    private final AtomicBoolean atrActive = new AtomicBoolean(false);

    public boolean isActive() {
        return atrActive.get();
    }

    public void activate() {
        atrActive.set(true);
    }

    public void deactivate() {
        atrActive.set(false);
    }
}
