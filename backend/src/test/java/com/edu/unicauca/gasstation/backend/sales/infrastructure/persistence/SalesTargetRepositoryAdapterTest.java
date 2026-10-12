package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence;

import com.edu.unicauca.gasstation.backend.sales.SalesTargetTestData;
import com.edu.unicauca.gasstation.backend.sales.exception.DuplicateSalesTargetException;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters.SalesTargetRepositoryAdapter;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.SalesTargetEntity;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.mappers.SalesTargetPersistenceMapper;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories.JpaSalesTargetRepository;
import jakarta.persistence.EntityManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesTargetRepositoryAdapterTest {
    @Mock JpaSalesTargetRepository repository;
    @Mock EntityManager entityManager;
    private final SalesTargetPersistenceMapper mapper = new SalesTargetPersistenceMapper();
    private SalesTargetRepositoryAdapter adapter;

    @BeforeEach
    void setup() { adapter = new SalesTargetRepositoryAdapter(repository, mapper, entityManager); }

    @Test
    void readsDatabaseGeneratedMetadataBeforeReturning() {
        var target = SalesTargetTestData.fuel(null, "1");
        when(repository.saveAndFlush(any())).thenAnswer(call -> {
            SalesTargetEntity entity = call.getArgument(0);
            entity.setId(1L);
            return entity;
        });
        doAnswer(call -> {
            SalesTargetEntity entity = call.getArgument(0);
            entity.setCreatedAt(Instant.parse("2026-08-01T00:00:00Z"));
            return null;
        }).when(entityManager).refresh(any());
        assertThat(adapter.register(target).createdAt()).isEqualTo(Instant.parse("2026-08-01T00:00:00Z"));
        var order = inOrder(repository, entityManager);
        order.verify(repository).saveAndFlush(any());
        order.verify(entityManager).refresh(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"uq_sales_targets_annual", "uq_sales_targets_monthly"})
    void translatesOnlyKnownUniqueIndexes(String index) {
        var target = SalesTargetTestData.fuel(null, "1");
        when(repository.saveAndFlush(any())).thenThrow(integrity("23505", index));
        assertThatThrownBy(() -> adapter.register(target)).isInstanceOf(DuplicateSalesTargetException.class);
        verifyNoInteractions(entityManager);
    }

    @Test
    void preservesOtherIntegrityErrors() {
        var target = SalesTargetTestData.fuel(null, "1");
        for (var error : List.of(integrity("23514", "ck_sales_targets_value"),
                integrity("23505", "other_unique_index"), integrity("23514", "uq_sales_targets_annual"),
                new DataIntegrityViolationException("unclassified error"))) {
            doThrow(error).when(repository).saveAndFlush(any());
            assertThatThrownBy(() -> adapter.register(target)).isSameAs(error);
        }
    }

    @Test
    void defendsDirectPortCallsAgainstRounding() {
        assertThatThrownBy(() -> adapter.register(SalesTargetTestData.fuel(null, "1E-13")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository, entityManager);
    }

    @Test
    void queriesBothAnnualNullMonthAndMonthlyIdentity() {
        for (Integer month : new Integer[]{null, 8}) {
            var target = SalesTargetTestData.fuel(month, "1");
            var entity = mapper.toEntity(target);
            entity.setId(1L);
            entity.setCreatedAt(Instant.parse("2026-08-01T00:00:00Z"));
            Short storedMonth = month == null ? null : month.shortValue();
            when(repository.findByTargetYearAndTargetMonthAndCategory(2026, storedMonth, target.identity().category()))
                    .thenReturn(Optional.of(entity));
            assertThat(adapter.findByIdentity(target.identity()).orElseThrow().target().identity()).isEqualTo(target.identity());
        }
        when(repository.findById(9L)).thenReturn(Optional.empty());
        when(repository.findByTargetYearOrderByTargetMonthAscCategoryAsc(2027)).thenReturn(List.of());
        assertThat(adapter.findById(9L)).isEmpty();
        assertThat(adapter.findByYear(2027)).isEmpty();
    }

    private static DataIntegrityViolationException integrity(String state, String name) {
        return new DataIntegrityViolationException("database error", new ConstraintViolationException(
                "database error", new SQLException("database error", state), "INSERT", name));
    }
}
