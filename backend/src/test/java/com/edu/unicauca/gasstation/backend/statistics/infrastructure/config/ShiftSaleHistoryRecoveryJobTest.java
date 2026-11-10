package com.edu.unicauca.gasstation.backend.statistics.infrastructure.config;

import com.edu.unicauca.gasstation.backend.statistics.application.ShiftSaleHistoryRecoveryService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class ShiftSaleHistoryRecoveryJobTest {
    @Test
    void aDatabaseOrRegistryOutageDoesNotCancelTheNextRecoveryPass() {
        var recovery = mock(ShiftSaleHistoryRecoveryService.class);
        doThrow(new IllegalStateException("registry offline")).doNothing().when(recovery).recoverPending();
        var job = new ShiftSaleHistoryRecoveryConfiguration.RecoveryJob(recovery);

        assertThatCode(job::run).doesNotThrowAnyException();
        assertThatCode(job::run).doesNotThrowAnyException();

        verify(recovery, times(2)).recoverPending();
    }
}