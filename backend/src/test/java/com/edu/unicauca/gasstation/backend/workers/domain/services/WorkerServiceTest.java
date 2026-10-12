package com.edu.unicauca.gasstation.backend.workers.domain.services;

import static com.edu.unicauca.gasstation.backend.workers.WorkerTestData.SUPPORT_ROLE_ID;
import static com.edu.unicauca.gasstation.backend.workers.WorkerTestData.TITULAR_ROLE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.shifts.ShiftExternalService;
import com.edu.unicauca.gasstation.backend.workers.WorkerTestData;
import com.edu.unicauca.gasstation.backend.workers.domain.models.Worker;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerChanges;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerDetail;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerFilter;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.WorkerRepository;
import com.edu.unicauca.gasstation.backend.workers.exception.DuplicateDocumentException;
import com.edu.unicauca.gasstation.backend.workers.exception.RoleNotFoundException;
import com.edu.unicauca.gasstation.backend.workers.exception.WorkerNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkerServiceTest {

    private static final UUID WORKER_ID = UUID.fromString("d3c8c22b-9a86-4678-a326-ac90b26057cb");
    private static final UUID UNKNOWN_ROLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private ShiftExternalService shiftExternalService;

    @InjectMocks
    private WorkerService workerService;

    // ---- create ----

    @Test
    void createSavesTheWorkerAndReturnsItWithItsRole() {
        Worker newWorker = WorkerTestData.newWorker("Ana Villamil", "0012345678", TITULAR_ROLE_ID);
        Worker saved = WorkerTestData.worker(WORKER_ID, "Ana Villamil", "0012345678", TITULAR_ROLE_ID, true);
        when(workerRepository.existsByDocument("0012345678")).thenReturn(false);
        stubRole(TITULAR_ROLE_ID);
        when(workerRepository.save(newWorker)).thenReturn(saved);

        WorkerDetail detail = workerService.create(newWorker);

        assertThat(detail.worker()).isSameAs(saved);
        assertThat(detail.role()).isEqualTo(WorkerTestData.titularRole());
    }

    @Test
    void createThrowsWhenDocumentAlreadyExists() {
        Worker newWorker = WorkerTestData.newWorker("Ana Villamil", "  0012345678 ", TITULAR_ROLE_ID);
        when(workerRepository.existsByDocument("0012345678")).thenReturn(true);

        assertThatThrownBy(() -> workerService.create(newWorker))
                .isInstanceOf(DuplicateDocumentException.class)
                .hasMessage(DuplicateDocumentException.MESSAGE);
        verify(workerRepository, never()).save(any());
        verifyNoInteractions(shiftExternalService);
    }

    @Test
    void createThrowsWhenRoleDoesNotExist() {
        Worker newWorker = WorkerTestData.newWorker("Ana Villamil", "0012345678", UNKNOWN_ROLE_ID);
        when(workerRepository.existsByDocument("0012345678")).thenReturn(false);
        when(shiftExternalService.getRolesByIds(Set.of(UNKNOWN_ROLE_ID))).thenReturn(Map.of());

        assertThatThrownBy(() -> workerService.create(newWorker))
                .isInstanceOf(RoleNotFoundException.class)
                .hasMessage("El rol indicado no existe");
        verify(workerRepository, never()).save(any());
    }

    // ---- update ----

    @Test
    void updateChangesDataAndKeepsAnInactiveWorkerInactive() {
        Worker existing = WorkerTestData.worker(WORKER_ID, "Ana Villamil", "0012345678", TITULAR_ROLE_ID, false);
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(existing));
        when(workerRepository.existsByDocumentAndIdNot("0012345679", WORKER_ID)).thenReturn(false);
        stubRole(SUPPORT_ROLE_ID);
        when(workerRepository.save(existing)).thenReturn(existing);

        WorkerDetail detail = workerService.update(WORKER_ID,
                new WorkerChanges("Ana Villamil Ruiz", "0012345679", SUPPORT_ROLE_ID));

        assertThat(detail.worker().getFullName()).isEqualTo("Ana Villamil Ruiz");
        assertThat(detail.worker().getDocument()).isEqualTo("0012345679");
        assertThat(detail.worker().getRoleId()).isEqualTo(SUPPORT_ROLE_ID);
        assertThat(detail.worker().isActive()).isFalse();
        assertThat(detail.role()).isEqualTo(WorkerTestData.supportRole());
    }

    @Test
    void updateThrowsWhenWorkerDoesNotExistBeforeCheckingTheDocument() {
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.empty());
        WorkerChanges changes = new WorkerChanges("Ana Villamil", "0012345678", TITULAR_ROLE_ID);

        assertThatThrownBy(() -> workerService.update(WORKER_ID, changes))
                .isInstanceOf(WorkerNotFoundException.class)
                .hasMessage("No se encontró el trabajador con id " + WORKER_ID);
        verify(workerRepository, never()).existsByDocumentAndIdNot(anyString(), any());
    }

    @Test
    void updateThrowsWhenDocumentBelongsToAnotherWorker() {
        Worker existing = WorkerTestData.worker(WORKER_ID, "Ana Villamil", "0012345678", TITULAR_ROLE_ID, true);
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(existing));
        when(workerRepository.existsByDocumentAndIdNot("34567890", WORKER_ID)).thenReturn(true);
        WorkerChanges changes = new WorkerChanges("Ana Villamil", "34567890", TITULAR_ROLE_ID);

        assertThatThrownBy(() -> workerService.update(WORKER_ID, changes))
                .isInstanceOf(DuplicateDocumentException.class);
        verify(workerRepository, never()).save(any());
    }

    @Test
    void updateThrowsWhenRoleDoesNotExist() {
        Worker existing = WorkerTestData.worker(WORKER_ID, "Ana Villamil", "0012345678", TITULAR_ROLE_ID, true);
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(existing));
        when(workerRepository.existsByDocumentAndIdNot("0012345678", WORKER_ID)).thenReturn(false);
        when(shiftExternalService.getRolesByIds(Set.of(UNKNOWN_ROLE_ID))).thenReturn(Map.of());
        WorkerChanges changes = new WorkerChanges("Ana Villamil", "0012345678", UNKNOWN_ROLE_ID);

        assertThatThrownBy(() -> workerService.update(WORKER_ID, changes))
                .isInstanceOf(RoleNotFoundException.class);
        verify(workerRepository, never()).save(any());
    }

    // ---- changeStatus / getById ----

    @Test
    void changeStatusDeactivatesTheWorker() {
        Worker existing = WorkerTestData.worker(WORKER_ID, "Ana Villamil", "0012345678", TITULAR_ROLE_ID, true);
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(existing));
        when(workerRepository.save(existing)).thenReturn(existing);
        stubRole(TITULAR_ROLE_ID);

        WorkerDetail detail = workerService.changeStatus(WORKER_ID, false);

        assertThat(detail.worker().isActive()).isFalse();
        verify(workerRepository).save(existing);
    }

    @Test
    void changeStatusThrowsWhenWorkerDoesNotExist() {
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workerService.changeStatus(WORKER_ID, false))
                .isInstanceOf(WorkerNotFoundException.class);
        verify(workerRepository, never()).save(any());
    }

    @Test
    void getByIdReturnsTheWorkerWithItsRole() {
        Worker existing = WorkerTestData.worker(WORKER_ID, "Carlos Restrepo", "1061789456", SUPPORT_ROLE_ID, true);
        when(workerRepository.findById(WORKER_ID)).thenReturn(Optional.of(existing));
        stubRole(SUPPORT_ROLE_ID);

        WorkerDetail detail = workerService.getById(WORKER_ID);

        assertThat(detail.worker()).isSameAs(existing);
        assertThat(detail.role().dispenser()).isEqualTo(3);
    }

    // ---- list ----

    @Test
    void listReadsAllRolesInASingleCallAndPairsEachWorkerWithItsRole() {
        Worker ana = WorkerTestData.worker(WORKER_ID, "Ana Villamil", "0012345678", TITULAR_ROLE_ID, true);
        Worker carlos = WorkerTestData.worker(UUID.randomUUID(), "Carlos Restrepo", "1061789456", SUPPORT_ROLE_ID, true);
        WorkerFilter filter = new WorkerFilter(true, null, "a");
        when(workerRepository.findAll(filter)).thenReturn(List.of(ana, carlos));
        when(shiftExternalService.getRolesByIds(Set.of(TITULAR_ROLE_ID, SUPPORT_ROLE_ID))).thenReturn(Map.of(
                TITULAR_ROLE_ID, WorkerTestData.titularRole(),
                SUPPORT_ROLE_ID, WorkerTestData.supportRole()));

        List<WorkerDetail> details = workerService.list(filter);

        verify(shiftExternalService, times(1)).getRolesByIds(anyCollection());
        assertThat(details).extracting(detail -> detail.role().name()).containsExactly("TITULAR", "APOYO");
    }

    @Test
    void listThrowsWhenARoleIsMissing() {
        Worker ana = WorkerTestData.worker(WORKER_ID, "Ana Villamil", "0012345678", TITULAR_ROLE_ID, true);
        WorkerFilter filter = new WorkerFilter(null, null, null);
        when(workerRepository.findAll(filter)).thenReturn(List.of(ana));
        when(shiftExternalService.getRolesByIds(Set.of(TITULAR_ROLE_ID))).thenReturn(Map.of());

        assertThatThrownBy(() -> workerService.list(filter))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(TITULAR_ROLE_ID.toString());
    }

    private void stubRole(UUID roleId) {
        when(shiftExternalService.getRolesByIds(Set.of(roleId))).thenReturn(Map.of(roleId,
                roleId.equals(TITULAR_ROLE_ID) ? WorkerTestData.titularRole() : WorkerTestData.supportRole()));
    }
}
