package com.edu.unicauca.gasstation.backend.workers.domain.models;

import static com.edu.unicauca.gasstation.backend.workers.WorkerTestData.TITULAR_ROLE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class WorkerChangesTest {

    @Test
    void stripsNameAndDocument() {
        WorkerChanges changes = new WorkerChanges("  Ana Villamil  ", " 0012345678 ", TITULAR_ROLE_ID);

        assertThat(changes).isEqualTo(new WorkerChanges("Ana Villamil", "0012345678", TITULAR_ROLE_ID));
    }

    @Test
    void rejectsNullData() {
        assertThatThrownBy(() -> new WorkerChanges(null, "0012345678", TITULAR_ROLE_ID)).hasMessage("fullName");
        assertThatThrownBy(() -> new WorkerChanges("Ana", null, TITULAR_ROLE_ID)).hasMessage("document");
        assertThatThrownBy(() -> new WorkerChanges("Ana", "0012345678", null)).hasMessage("roleId");
    }
}
