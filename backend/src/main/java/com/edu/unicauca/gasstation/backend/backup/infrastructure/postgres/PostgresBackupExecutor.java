package com.edu.unicauca.gasstation.backend.backup.infrastructure.postgres;

import com.edu.unicauca.gasstation.backend.backup.application.ports.BackupExecutor;
import com.edu.unicauca.gasstation.backend.backup.exception.BackupException;
import com.edu.unicauca.gasstation.backend.backup.infrastructure.config.BackupProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Runs pg_dump and pg_restore as child processes. The password is sent through the environment,
 * never as a command line argument.
 *
 * @author javiersolanop777
 */
@Component
public class PostgresBackupExecutor implements BackupExecutor {

    private static final int MAX_OUTPUT_BYTES = 32_768;

    private final BackupProperties atrProperties;
    private final String atrHost;
    private final String atrPort;
    private final String atrDatabase;
    private final String atrUser;
    private final String atrPassword;

    public PostgresBackupExecutor(
            BackupProperties prmProperties,
            @Value("${spring.datasource.url}") String prmUrl,
            @Value("${spring.datasource.username}") String prmUser,
            @Value("${spring.datasource.password:}") String prmPassword) {
        this.atrProperties = prmProperties;
        this.atrUser = prmUser;
        this.atrPassword = prmPassword;

        String objUrl = prmUrl.startsWith("jdbc:") ? prmUrl.substring(5) : prmUrl;
        URI objUri = URI.create(objUrl);
        this.atrHost = objUri.getHost() == null ? "localhost" : objUri.getHost();
        this.atrPort = objUri.getPort() == -1 ? "5432" : String.valueOf(objUri.getPort());
        String objPath = objUri.getPath();
        this.atrDatabase = objPath == null || objPath.length() <= 1 ? "postgres" : objPath.substring(1);
    }

    @Override
    public void dump(Path prmTarget) {
        List<String> objCommand = new ArrayList<>();
        objCommand.add(atrProperties.getDumpCommand());
        objCommand.add("--format=custom");
        objCommand.add("--compress=9");
        objCommand.add("--no-owner");
        objCommand.add("--no-privileges");
        objCommand.add("--encoding=UTF8");
        objCommand.add("--exclude-table=backup_audit");
        objCommand.add("--host=" + atrHost);
        objCommand.add("--port=" + atrPort);
        objCommand.add("--username=" + atrUser);
        objCommand.add("--dbname=" + atrDatabase);
        objCommand.add("--file=" + prmTarget.toAbsolutePath());
        run(objCommand, "dump");
    }

    @Override
    public void restore(Path prmSource) {
        List<String> objCommand = new ArrayList<>();
        objCommand.add(atrProperties.getRestoreCommand());
        objCommand.add("--clean");
        objCommand.add("--if-exists");
        objCommand.add("--no-owner");
        objCommand.add("--no-privileges");
        objCommand.add("--single-transaction");
        objCommand.add("--exit-on-error");
        objCommand.add("--host=" + atrHost);
        objCommand.add("--port=" + atrPort);
        objCommand.add("--username=" + atrUser);
        objCommand.add("--dbname=" + atrDatabase);
        objCommand.add(prmSource.toAbsolutePath().toString());
        run(objCommand, "restore");
    }

    private void run(List<String> prmCommand, String prmOperation) {
        ProcessBuilder objBuilder = new ProcessBuilder(prmCommand);
        objBuilder.redirectErrorStream(true);
        objBuilder.environment().put("PGPASSWORD", atrPassword);
        objBuilder.environment().putIfAbsent("PGCONNECT_TIMEOUT", "10");

        Process objProcess;
        try {
            objProcess = objBuilder.start();
        } catch (IOException prmException) {
            throw new BackupException(BackupException.Reason.IO_ERROR,
                    "Could not start " + prmOperation + ": " + prmException.getMessage(), prmException);
        }

        ByteArrayOutputStream objOutput = new ByteArrayOutputStream();
        Thread objReader = Thread.ofVirtual().start(() -> readOutput(objProcess.getInputStream(), objOutput));

        boolean objFinished;
        try {
            objFinished = objProcess.waitFor(atrProperties.getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException prmException) {
            Thread.currentThread().interrupt();
            objProcess.destroyForcibly();
            throw new BackupException(BackupException.Reason.IO_ERROR,
                    "The " + prmOperation + " was interrupted", prmException);
        }

        if (!objFinished) {
            objProcess.destroyForcibly();
            throw new BackupException(BackupException.Reason.IO_ERROR,
                    "The " + prmOperation + " took more than " + atrProperties.getTimeoutSeconds() + " seconds");
        }

        try {
            objReader.join(5_000);
        } catch (InterruptedException prmException) {
            Thread.currentThread().interrupt();
        }

        String objMessage = objOutput.toString(StandardCharsets.UTF_8).trim();
        if (objProcess.exitValue() != 0) {
            BackupException.Reason objReason = "dump".equals(prmOperation)
                    ? BackupException.Reason.DUMP_FAILED
                    : BackupException.Reason.RESTORE_FAILED;
            String objDetail = objMessage.isEmpty() ? "unknown error" : objMessage;
            throw new BackupException(objReason, "pg_" + prmOperation + " failed: " + objDetail);
        }
    }

    private void readOutput(InputStream prmInput, ByteArrayOutputStream prmOutput) {
        try (InputStream objInput = prmInput) {
            byte[] arrChunk = new byte[4_096];
            int varRead;
            while ((varRead = objInput.read(arrChunk)) != -1) {
                int varFree = MAX_OUTPUT_BYTES - prmOutput.size();
                if (varFree > 0) {
                    prmOutput.write(arrChunk, 0, Math.min(varRead, varFree));
                }
            }
        } catch (IOException objException) {
            // The process may close the stream when it is destroyed. The exit code is checked later.
        }
    }
}
