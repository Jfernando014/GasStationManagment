package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.SalesTargetTestData;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.SalesTargetRepository;
import com.edu.unicauca.gasstation.backend.sales.exception.DuplicateSalesTargetException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesTargetServiceTest {
    @Mock SalesTargetRepository repository;
    @InjectMocks SalesTargetService service;

    @Test
    void registersExactValueWithoutNormalizingIt() {
        var target = SalesTargetTestData.fuel(8, "1.2300000000000");
        var stored = SalesTargetTestData.stored(target);
        when(repository.register(target)).thenReturn(stored);
        assertThat(service.register(target)).isSameAs(stored);
        verify(repository).register(target);
        assertThat(target.value().scale()).isEqualTo(13);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1E18", "1E-13"})
    void rejectsUnrepresentableValuesBeforeAnyRepositoryCall(String value) {
        assertThatThrownBy(() -> service.register(SalesTargetTestData.fuel(null, value)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsNullBeforeAnyRepositoryCall() {
        assertThatThrownBy(() -> service.register(null)).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void delegatesQueriesIncludingAbsentResults() {
        var target = SalesTargetTestData.fuel(null, "1");
        var stored = SalesTargetTestData.stored(target);
        when(repository.findById(1L)).thenReturn(Optional.of(stored));
        when(repository.findById(2L)).thenReturn(Optional.empty());
        when(repository.findByIdentity(target.identity())).thenReturn(Optional.of(stored));
        when(repository.findByYear(2026)).thenReturn(List.of(stored));
        assertThat(service.findById(1L)).contains(stored);
        assertThat(service.findById(2L)).isEmpty();
        assertThat(service.findByIdentity(target.identity())).contains(stored);
        assertThat(service.findByYear(2026)).containsExactly(stored);
    }

    @Test
    void propagatesSpecificDuplicateError() {
        var target = SalesTargetTestData.fuel(null, "1");
        var error = new DuplicateSalesTargetException(target.identity(), null);
        when(repository.register(target)).thenThrow(error);
        assertThatThrownBy(() -> service.register(target)).isSameAs(error);
    }
}
