package com.edu.unicauca.gasstation.backend.workers.domain.models;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WorkerFilterTest {

    @Test
    void hasSearchIsFalseWithoutText() {
        assertThat(new WorkerFilter(null, null, null).hasSearch()).isFalse();
        assertThat(new WorkerFilter(null, null, "").hasSearch()).isFalse();
        assertThat(new WorkerFilter(null, null, "   ").hasSearch()).isFalse();
    }

    @Test
    void hasSearchIsTrueWithText() {
        assertThat(new WorkerFilter(null, null, " ana ").hasSearch()).isTrue();
    }
}
