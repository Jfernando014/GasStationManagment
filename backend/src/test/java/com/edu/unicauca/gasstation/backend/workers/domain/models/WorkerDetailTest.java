package com.edu.unicauca.gasstation.backend.workers.domain.models;

import static com.edu.unicauca.gasstation.backend.workers.WorkerTestData.TITULAR_ROLE_ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edu.unicauca.gasstation.backend.workers.WorkerTestData;
import org.junit.jupiter.api.Test;

class WorkerDetailTest {

    @Test
    void rejectsNullWorkerOrRole() {
        Worker worker = WorkerTestData.newWorker("Ana Villamil", "0012345678", TITULAR_ROLE_ID);

        assertThatThrownBy(() -> new WorkerDetail(null, WorkerTestData.titularRole())).hasMessage("worker");
        assertThatThrownBy(() -> new WorkerDetail(worker, null)).hasMessage("role");
    }
}
