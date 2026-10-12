package com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence;

import static com.edu.unicauca.gasstation.backend.workers.WorkerTestData.SUPPORT_ROLE_ID;
import static com.edu.unicauca.gasstation.backend.workers.WorkerTestData.TITULAR_ROLE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edu.unicauca.gasstation.backend.workers.WorkerTestData;
import com.edu.unicauca.gasstation.backend.workers.domain.models.Worker;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerFilter;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.WorkerRepository;
import com.edu.unicauca.gasstation.backend.workers.exception.DuplicateDocumentException;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.adapters.WorkerRepositoryImpl;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.mappers.WorkerPersistenceMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({WorkerRepositoryImpl.class, WorkerPersistenceMapper.class})
class WorkerRepositoryTest {

    private static final WorkerFilter NO_FILTER = new WorkerFilter(null, null, null);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private WorkerPersistenceMapper persistenceMapper;

    @Autowired
    private TestEntityManager entityManager;

    // ---- save ----

    @Test
    void saveGeneratesIdAndPersistsAllFieldsWithLeadingZeros() {
        Worker saved = save("Ana Villamil", "0012345678", TITULAR_ROLE_ID, true);
        clearContext();

        Worker found = workerRepository.findById(saved.getId()).orElseThrow();

        assertThat(found).usingRecursiveComparison().isEqualTo(saved);
        assertThat(found.getDocument()).isEqualTo("0012345678");
    }

    @Test
    void saveTranslatesTheUniqueDocumentConstraintToDuplicateDocumentException() {
        save("Ana Villamil", "0012345678", TITULAR_ROLE_ID, true);
        Worker duplicate = WorkerTestData.newWorker("Carlos Restrepo", "0012345678", SUPPORT_ROLE_ID);

        assertThatThrownBy(() -> workerRepository.save(duplicate))
                .isInstanceOf(DuplicateDocumentException.class);
    }

    @Test
    void savingAnExistingWorkerUpdatesInsteadOfInserting() {
        Worker saved = save("Ana Villamil", "0012345678", TITULAR_ROLE_ID, true);
        clearContext();

        Worker existing = workerRepository.findById(saved.getId()).orElseThrow();
        existing.updateDetails("Ana Villamil Ruiz", "0012345679", SUPPORT_ROLE_ID);
        existing.deactivate();
        workerRepository.save(existing);
        clearContext();

        List<Worker> all = workerRepository.findAll(NO_FILTER);
        assertThat(all).singleElement().satisfies(worker -> {
            assertThat(worker.getId()).isEqualTo(saved.getId());
            assertThat(worker.getFullName()).isEqualTo("Ana Villamil Ruiz");
            assertThat(worker.isActive()).isFalse();
        });
    }

    // ---- lookups ----

    @Test
    void findByIdReturnsEmptyWhenTheWorkerDoesNotExist() {
        assertThat(workerRepository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findAllByIdsReturnsOnlyTheExistingWorkers() {
        Worker ana = save("Ana Villamil", "0012345678", TITULAR_ROLE_ID, true);
        save("Luisa Paz", "34567890", TITULAR_ROLE_ID, false);
        clearContext();

        assertThat(workerRepository.findAllByIds(List.of())).isEmpty();
        assertThat(workerRepository.findAllByIds(List.of(ana.getId(), UUID.randomUUID())))
                .extracting(Worker::getId).containsExactly(ana.getId());
    }

    @Test
    void documentChecksExcludeTheWorkerItself() {
        Worker ana = save("Ana Villamil", "0012345678", TITULAR_ROLE_ID, true);
        Worker carlos = save("Carlos Restrepo", "1061789456", SUPPORT_ROLE_ID, true);

        assertThat(workerRepository.existsByDocument("0012345678")).isTrue();
        assertThat(workerRepository.existsByDocument("12345678")).as("leading zeros matter").isFalse();
        assertThat(workerRepository.existsByDocumentAndIdNot("0012345678", ana.getId())).isFalse();
        assertThat(workerRepository.existsByDocumentAndIdNot("0012345678", carlos.getId())).isTrue();
    }

    // ---- findAll(WorkerFilter) ----

    @Test
    void findAllWithoutFiltersReturnsEveryWorkerOrderedByName() {
        saveSampleWorkers();

        assertThat(workerRepository.findAll(NO_FILTER)).extracting(Worker::getFullName)
                .containsExactly("Ana Villamil", "Bruno Diaz", "Carlos Restrepo", "Luisa Paz");
    }

    @Test
    void findAllCombinesStatusAndRoleFilters() {
        saveSampleWorkers();

        assertThat(workerRepository.findAll(new WorkerFilter(true, SUPPORT_ROLE_ID, null)))
                .extracting(Worker::getFullName).containsExactly("Bruno Diaz", "Carlos Restrepo");
        assertThat(workerRepository.findAll(new WorkerFilter(false, null, null)))
                .extracting(Worker::getFullName).containsExactly("Luisa Paz");
    }

    @Test
    void findAllSearchesNameIgnoringCaseAndDocumentWithStrippedText() {
        saveSampleWorkers();

        assertThat(workerRepository.findAll(new WorkerFilter(null, null, "  VILLA "))).extracting(Worker::getFullName)
                .containsExactly("Ana Villamil");
        assertThat(workerRepository.findAll(new WorkerFilter(null, null, "1061"))).extracting(Worker::getFullName)
                .containsExactly("Carlos Restrepo");
        assertThat(workerRepository.findAll(new WorkerFilter(null, null, "   "))).as("blank search is ignored")
                .hasSize(4);
    }

    @Test
    void findAllTreatsPercentAndUnderscoreAsLiteralText() {
        save("Ana Lopez", "0012345678", TITULAR_ROLE_ID, true);
        save("Ana_lopez Ruiz", "1061789456", TITULAR_ROLE_ID, true);
        save("Cien% Puro", "34567890", TITULAR_ROLE_ID, true);

        assertThat(workerRepository.findAll(new WorkerFilter(null, null, "a_l"))).extracting(Worker::getFullName)
                .containsExactly("Ana_lopez Ruiz");
        assertThat(workerRepository.findAll(new WorkerFilter(null, null, "%"))).extracting(Worker::getFullName)
                .containsExactly("Cien% Puro");
    }

    @Test
    void persistenceMapperRoundTripKeepsEveryField() {
        Worker worker = WorkerTestData.worker(UUID.randomUUID(), "Julian CalcEdo", "0012345678", SUPPORT_ROLE_ID, false);

        assertThat(persistenceMapper.toDomain(persistenceMapper.toEntity(worker)))
                .usingRecursiveComparison().isEqualTo(worker);
    }

    private Worker save(String fullName, String document, UUID roleId, boolean active) {
        Worker worker = WorkerTestData.newWorker(fullName, document, roleId);
        if (!active) {
            worker.deactivate();
        }
        return workerRepository.save(worker);
    }

    /** Ana and Luisa are TITULAR, Bruno and Carlos APOYO; Luisa is inactive. */
    private void saveSampleWorkers() {
        save("Luisa Paz", "34567890", TITULAR_ROLE_ID, false);
        save("Carlos Restrepo", "1061789456", SUPPORT_ROLE_ID, true);
        save("Ana Villamil", "0012345678", TITULAR_ROLE_ID, true);
        save("Bruno Diaz", "52123456", SUPPORT_ROLE_ID, true);
        clearContext();
    }

    private void clearContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
