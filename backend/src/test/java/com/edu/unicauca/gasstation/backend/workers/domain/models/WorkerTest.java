package com.edu.unicauca.gasstation.backend.workers.domain.models;

import static com.edu.unicauca.gasstation.backend.workers.WorkerTestData.SUPPORT_ROLE_ID;
import static com.edu.unicauca.gasstation.backend.workers.WorkerTestData.TITULAR_ROLE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edu.unicauca.gasstation.backend.workers.WorkerTestData;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WorkerTest {

    @Test
    void newWorkerHasNoIdAndStartsActive() {
        Worker worker = new Worker("Ana Villamil", "0012345678", TITULAR_ROLE_ID);

        assertThat(worker.getId()).isNull();
        assertThat(worker.isActive()).isTrue();
    }

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource(delimiter = '|', value = {
            "  ana    milena   VILLAMIL  | Ana Milena Villamil",
            "JOSÉ ñuñez                  | José Ñuñez",
            "maría-josé torre-gómez      | María-José Torre-Gómez"
    })
    void normalizesSpacesCaseAccentsAndHyphens(String input, String expected) {
        assertThat(new Worker(input, "0012345678", TITULAR_ROLE_ID).getFullName()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource(delimiter = '|', value = {
            "maría DEL pilar DE LA torre | María del Pilar de la Torre",
            "de la cruz ana              | De la Cruz Ana"
    })
    void keepsConnectorsInLowerCaseExceptAsFirstWord(String input, String expected) {
        assertThat(new Worker(input, "0012345678", TITULAR_ROLE_ID).getFullName()).isEqualTo(expected);
    }

    @Test
    void apostropheDoesNotCapitalizeTheFollowingLetter() {
        // Current behavior: only the first letter of the word is capitalized ("O'brien", not "O'Brien")
        assertThat(new Worker("liam o'brien", "0012345678", TITULAR_ROLE_ID).getFullName()).isEqualTo("Liam O'brien");
    }

    @Test
    void stripsTheDocumentAndKeepsLeadingZeros() {
        assertThat(new Worker("Ana Villamil", "  0012345678  ", TITULAR_ROLE_ID).getDocument()).isEqualTo("0012345678");
    }

    @Test
    void rejectsNullData() {
        assertThatThrownBy(() -> new Worker(null, "0012345678", TITULAR_ROLE_ID)).hasMessage("fullName");
        assertThatThrownBy(() -> new Worker("Ana", null, TITULAR_ROLE_ID)).hasMessage("document");
        assertThatThrownBy(() -> new Worker("Ana", "0012345678", null)).hasMessage("roleId");
        assertThatThrownBy(() -> Worker.restore(null, "Ana", "0012345678", TITULAR_ROLE_ID, true)).hasMessage("id");
    }

    @Test
    void restoreKeepsIdStatusAndStoredData() {
        UUID id = UUID.randomUUID();

        Worker worker = Worker.restore(id, "Julian CalcEdo", "1019023413", SUPPORT_ROLE_ID, false);

        assertThat(worker.getId()).isEqualTo(id);
        assertThat(worker.isActive()).isFalse();
        assertThat(worker.getFullName()).as("restore keeps the name as stored").isEqualTo("Julian CalcEdo");
    }

    @Test
    void updateDetailsNormalizesWithoutTouchingIdOrStatus() {
        UUID id = UUID.randomUUID();
        Worker worker = WorkerTestData.worker(id, "Ana Villamil", "0012345678", TITULAR_ROLE_ID, false);

        worker.updateDetails("  luisa   DE LA paz ", " 34567890 ", SUPPORT_ROLE_ID);

        assertThat(worker.getId()).isEqualTo(id);
        assertThat(worker.isActive()).isFalse();
        assertThat(worker.getFullName()).isEqualTo("Luisa de la Paz");
        assertThat(worker.getDocument()).isEqualTo("34567890");
        assertThat(worker.getRoleId()).isEqualTo(SUPPORT_ROLE_ID);
    }

    @Test
    void activateAndDeactivateChangeTheStatusAndCanBeRepeated() {
        Worker worker = new Worker("Ana Villamil", "0012345678", TITULAR_ROLE_ID);

        worker.deactivate();
        worker.deactivate();
        assertThat(worker.isActive()).isFalse();

        worker.activate();
        assertThat(worker.isActive()).isTrue();
    }
}
