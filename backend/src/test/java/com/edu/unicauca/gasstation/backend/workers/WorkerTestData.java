package com.edu.unicauca.gasstation.backend.workers;

import com.edu.unicauca.gasstation.backend.shifts.RoleInfo;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.CreateWorkerRequest;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.UpdateWorkerRequest;
import com.edu.unicauca.gasstation.backend.workers.domain.models.Worker;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerDetail;
import java.util.UUID;

public final class WorkerTestData {

    public static final UUID TITULAR_ROLE_ID = UUID.fromString("7efc5d6e-e6b5-4093-9348-e062a03eb51a");
    public static final UUID SUPPORT_ROLE_ID = UUID.fromString("2bb7696c-c19e-4b0b-b8ac-bc2cd46f8e7e");

    private WorkerTestData() {
    }

    /** TITULAR role, dispenser 1. */
    public static RoleInfo titularRole() {
        return new RoleInfo(TITULAR_ROLE_ID, "TITULAR", 1);
    }

    /** APOYO role, dispenser 3. */
    public static RoleInfo supportRole() {
        return new RoleInfo(SUPPORT_ROLE_ID, "APOYO", 3);
    }

    /** A new worker, not saved yet (no id). */
    public static Worker newWorker(String fullName, String document, UUID roleId) {
        return new Worker(fullName, document, roleId);
    }

    /** A worker that already exists, as read from the database. */
    public static Worker worker(UUID id, String fullName, String document, UUID roleId, boolean active) {
        return Worker.restore(id, fullName, document, roleId, active);
    }

    public static WorkerDetail detail(Worker worker, RoleInfo role) {
        return new WorkerDetail(worker, role);
    }

    public static CreateWorkerRequest createRequest(String fullName, String document, UUID roleId) {
        return new CreateWorkerRequest(fullName, document, roleId);
    }

    public static UpdateWorkerRequest updateRequest(String fullName, String document, UUID roleId) {
        return new UpdateWorkerRequest(fullName, document, roleId);
    }
}
