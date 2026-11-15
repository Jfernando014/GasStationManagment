package com.edu.unicauca.gasstation.backend.backup.infrastructure.restart;

import com.edu.unicauca.gasstation.backend.backup.application.ports.BackupRestarter;
import com.edu.unicauca.gasstation.backend.backup.infrastructure.config.BackupProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Restarts DevTools in development and stops the process in production, where the container
 * restart policy starts it again.
 *
 * @author javiersolanop777
 */
@Slf4j
@Component
public class ApplicationRestartService implements BackupRestarter {

    private final BackupProperties atrProperties;
    private final Environment atrEnvironment;
    private final ApplicationContext atrContext;

    public ApplicationRestartService(BackupProperties prmProperties, Environment prmEnvironment,
            ApplicationContext prmContext) {
        this.atrProperties = prmProperties;
        this.atrEnvironment = prmEnvironment;
        this.atrContext = prmContext;
    }

    @Override
    public void restartAfterRestore() {
        if (!atrProperties.isRestartAfterRestore()) {
            log.info("Restart after restore is disabled");
            return;
        }
        Thread.ofPlatform().name("backup-restart").daemon(false).start(this::restart);
    }

    private void restart() {
        sleep();
        if (isDevToolsEnabled() && restartWithDevTools()) {
            return;
        }
        log.info("Stopping the application so the container can start it again");
        int varExitCode = SpringApplication.exit(atrContext, () -> 0);
        System.exit(varExitCode);
    }

    private boolean isDevToolsEnabled() {
        Boolean objEnabled = atrEnvironment.getProperty("spring.devtools.restart.enabled", Boolean.class);
        if (objEnabled != null) {
            return objEnabled;
        }
        try {
            Class.forName("org.springframework.boot.devtools.restart.Restarter");
            return true;
        } catch (ClassNotFoundException prmException) {
            return false;
        }
    }

    private boolean restartWithDevTools() {
        try {
            Class<?> objRestarterClass = Class.forName("org.springframework.boot.devtools.restart.Restarter");
            Object objRestarter = objRestarterClass.getMethod("getInstance").invoke(null);
            objRestarterClass.getMethod("restart").invoke(objRestarter);
            return true;
        } catch (ReflectiveOperationException prmException) {
            log.warn("DevTools could not restart the application", prmException);
            return false;
        }
    }

    private void sleep() {
        try {
            Thread.sleep(1_000);
        } catch (InterruptedException prmException) {
            Thread.currentThread().interrupt();
        }
    }
}
