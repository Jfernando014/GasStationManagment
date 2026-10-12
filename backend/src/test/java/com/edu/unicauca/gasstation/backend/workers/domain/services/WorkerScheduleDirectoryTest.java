package com.edu.unicauca.gasstation.backend.workers.domain.services;

import static com.edu.unicauca.gasstation.backend.workers.WorkerTestData.SUPPORT_ROLE_ID;
import static com.edu.unicauca.gasstation.backend.workers.WorkerTestData.TITULAR_ROLE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.shifts.ScheduleWorkerInfo;
import com.edu.unicauca.gasstation.backend.workers.WorkerTestData;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.WorkerRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkerScheduleDirectoryTest {

    private static final UUID ANA_ID = UUID.fromString("d3c8c22b-9a86-4678-a326-ac90b26057cb");
    private static final UUID LUISA_ID = UUID.fromString("5f0b1c9e-2d1a-4b8e-9f3c-7a6d5e4c3b2a");

    @Mock
    private WorkerRepository workerRepository;

    @InjectMocks
    private WorkerScheduleDirectory workerScheduleDirectory;

    @Test
    void convertsEachWorkerToScheduleInfoKeepingTheStatus() {
        List<UUID> ids = List.of(ANA_ID, LUISA_ID);
        when(workerRepository.findAllByIds(ids)).thenReturn(List.of(
                WorkerTestData.worker(ANA_ID, "Ana Villamil", "0012345678", TITULAR_ROLE_ID, true),
                WorkerTestData.worker(LUISA_ID, "Luisa Paz", "34567890", SUPPORT_ROLE_ID, false)));

        Map<UUID, ScheduleWorkerInfo> workers = workerScheduleDirectory.getWorkersByIds(ids);

        assertThat(workers).containsExactlyInAnyOrderEntriesOf(Map.of(
                ANA_ID, new ScheduleWorkerInfo(ANA_ID, "Ana Villamil", TITULAR_ROLE_ID, true),
                LUISA_ID, new ScheduleWorkerInfo(LUISA_ID, "Luisa Paz", SUPPORT_ROLE_ID, false)));
    }

    @Test
    void unknownIdIsNotInTheMap() {
        UUID unknownId = UUID.randomUUID();
        when(workerRepository.findAllByIds(List.of(ANA_ID, unknownId))).thenReturn(List.of(
                WorkerTestData.worker(ANA_ID, "Ana Villamil", "0012345678", TITULAR_ROLE_ID, true)));

        assertThat(workerScheduleDirectory.getWorkersByIds(List.of(ANA_ID, unknownId))).containsOnlyKeys(ANA_ID);
    }
}
