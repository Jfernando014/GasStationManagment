package com.edu.unicauca.gasstation.backend.workers.infrastructure.mappers;

import static com.edu.unicauca.gasstation.backend.workers.WorkerTestData.SUPPORT_ROLE_ID;
import static com.edu.unicauca.gasstation.backend.workers.WorkerTestData.TITULAR_ROLE_ID;
import static org.assertj.core.api.Assertions.assertThat;

import com.edu.unicauca.gasstation.backend.workers.WorkerTestData;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.WorkerResponse;
import com.edu.unicauca.gasstation.backend.workers.domain.models.Worker;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerChanges;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WorkerMapperTest {

    private final WorkerMapper mapper = new WorkerMapperImpl();

    @Test
    void createRequestBecomesAnActiveWorkerWithNormalizedName() {
        Worker worker = mapper.toDomain(WorkerTestData.createRequest("  ana   DEL valle ", "0012345678", TITULAR_ROLE_ID));

        assertThat(worker.getId()).isNull();
        assertThat(worker.isActive()).isTrue();
        assertThat(worker.getFullName()).isEqualTo("Ana del Valle");
        assertThat(worker.getDocument()).isEqualTo("0012345678");
        assertThat(worker.getRoleId()).isEqualTo(TITULAR_ROLE_ID);
    }

    @Test
    void updateRequestBecomesWorkerChanges() {
        WorkerChanges changes = mapper.toDomain(WorkerTestData.updateRequest(" Ana Ruiz ", " 0012345679 ", SUPPORT_ROLE_ID));

        assertThat(changes).isEqualTo(new WorkerChanges("Ana Ruiz", "0012345679", SUPPORT_ROLE_ID));
    }

    @Test
    void detailBecomesResponseWithRoleNameDispenserAndStatus() {
        UUID id = UUID.randomUUID();
        Worker worker = WorkerTestData.worker(id, "Carlos Restrepo", "1061789456", SUPPORT_ROLE_ID, false);

        WorkerResponse response = mapper.toResponse(WorkerTestData.detail(worker, WorkerTestData.supportRole()));

        assertThat(response).isEqualTo(new WorkerResponse(
                id, "Carlos Restrepo", "1061789456", SUPPORT_ROLE_ID, "APOYO", 3, false));
    }
}
