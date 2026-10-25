package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.Role;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.RoleRepository;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.adapters.RoleRepositoryImpl;
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
@Import({RoleRepositoryImpl.class})
class RoleRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveGeneratesTheIdAndFindByNameReadsItBack() {
        Role saved = roleRepository.save(new Role(null, "TITULAR", 1));
        clearContext();

        assertThat(saved.getId()).isNotNull();
        assertThat(roleRepository.findByName("TITULAR")).get()
                .usingRecursiveComparison().isEqualTo(saved);
        assertThat(roleRepository.findByName("SUPERVISOR")).isEmpty();
    }

    @Test
    void findAllByIdsReturnsOnlyTheExistingRoles() {
        Role titular = roleRepository.save(new Role(null, "TITULAR", 1));
        roleRepository.save(new Role(null, "APOYO", 3));
        clearContext();

        assertThat(roleRepository.findAllByIds(List.of())).isEmpty();
        assertThat(roleRepository.findAllByIds(List.of(titular.getId(), UUID.randomUUID())))
                .extracting(Role::getName).containsExactly("TITULAR");
    }

    private void clearContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
