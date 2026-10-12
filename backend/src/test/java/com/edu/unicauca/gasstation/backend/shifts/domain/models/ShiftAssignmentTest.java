package com.edu.unicauca.gasstation.backend.shifts.domain.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edu.unicauca.gasstation.backend.shifts.ShiftTestData;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShiftAssignmentTest {

    private static final UUID WORKER_ID = UUID.fromString("d3c8c22b-9a86-4678-a326-ac90b26057cb");
    private static final LocalDate DATE = LocalDate.of(2026, 11, 2);

    @Test
    void newAssignmentHasNoIdAndStripsTheNote() {
        ShiftAssignment assignment = new ShiftAssignment(WORKER_ID, DATE, ShiftTestData.shiftCode("DIA"), "  Cita médica  ");

        assertThat(assignment.getId()).isNull();
        assertThat(assignment.getNote()).isEqualTo("Cita médica");
    }

    @Test
    void blankNoteIsStoredAsNull() {
        assertThat(new ShiftAssignment(WORKER_ID, DATE, ShiftTestData.shiftCode("DIA"), "   ").getNote()).isNull();
    }

    @Test
    void rejectsNullData() {
        ShiftCode dia = ShiftTestData.shiftCode("DIA");

        assertThatThrownBy(() -> new ShiftAssignment(null, DATE, dia, null)).hasMessage("workerId");
        assertThatThrownBy(() -> new ShiftAssignment(WORKER_ID, null, dia, null)).hasMessage("workDate");
        assertThatThrownBy(() -> new ShiftAssignment(WORKER_ID, DATE, null, null)).hasMessage("shiftCode");
        assertThatThrownBy(() -> ShiftAssignment.restore(null, WORKER_ID, DATE, dia, null)).hasMessage("id");
    }

    @Test
    void changeReplacesTheCodeAndCanRemoveTheNote() {
        UUID id = UUID.randomUUID();
        ShiftAssignment assignment = ShiftAssignment.restore(id, WORKER_ID, DATE, ShiftTestData.shiftCode("DIA"), "Inicial");

        assignment.change(ShiftTestData.shiftCode("NOCHE"), null);

        assertThat(assignment.getId()).isEqualTo(id);
        assertThat(assignment.getShiftCode().getCode()).isEqualTo("NOCHE");
        assertThat(assignment.getNote()).isNull();
    }
}
