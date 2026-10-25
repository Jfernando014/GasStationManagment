package com.edu.unicauca.gasstation.backend.shifts.infrastructure.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.shifts.ShiftTestData;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.Role;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.RoleRepository;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftCodeRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The repositories are mocks backed by maps, so the initializer sees what it saved (it reads the roles and the
 * paired codes back after saving them).
 */
@ExtendWith(MockitoExtension.class)
class ShiftDataInitializerTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ShiftCodeRepository shiftCodeRepository;

    @InjectMocks
    private ShiftDataInitializer initializer;

    private final Map<String, Role> roles = new HashMap<>();
    private final Map<String, ShiftCode> codes = new HashMap<>();

    @BeforeEach
    void backRepositoriesWithMaps() {
        when(roleRepository.findByName(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(roles.get(invocation.<String>getArgument(0))));
        when(shiftCodeRepository.findByCode(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(codes.get(invocation.<String>getArgument(0))));
    }

    @Test
    void loadsTheRolesAndTheElevenCodesOnAnEmptyDatabase() {
        stubRoleSave();
        stubCodeSave();

        initializer.run(null);

        ArgumentCaptor<Role> savedRoles = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository, times(2)).save(savedRoles.capture());
        assertThat(savedRoles.getAllValues()).extracting(Role::getName, Role::getDispenser)
                .containsExactlyInAnyOrder(tuple("TITULAR", 1), tuple("APOYO", 3));

        Map<String, ShiftCode> saved = savedCodes();
        assertThat(saved).containsOnlyKeys(ShiftTestData.CATALOG_CODES);
        assertThat(saved.values()).allMatch(ShiftCode::isActive);
        assertThat(saved.values().stream().collect(Collectors.toMap(ShiftCode::getCode, ShiftCode::totalHours)))
                .containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                        Map.entry("DIA", 12), Map.entry("DIA6", 6), Map.entry("NOCHE", 12), Map.entry("9-6", 9),
                        Map.entry("12-7", 7), Map.entry("6-9y5-9", 7), Map.entry("2-9pm", 7), Map.entry("6-9am", 3),
                        Map.entry("2-6pm", 4), Map.entry("6-9y2-6", 7), Map.entry("DESCANSO", 0)));
        assertThat(saved.get("12-7").getPairedWithId()).isEqualTo(saved.get("DIA6").getId());
        assertThat(saved.get("6-9y5-9").getPairedWithId()).isEqualTo(saved.get("9-6").getId());
        assertThat(List.of("2-9pm", "6-9am", "2-6pm", "6-9y2-6"))
                .allSatisfy(code -> assertThat(saved.get(code).getPairedWithId()).as(code).isNull());
        assertThat(saved.values()).filteredOn(ShiftCode::isRequiresSupport).extracting(ShiftCode::getCode)
                .containsExactlyInAnyOrder("DIA6", "9-6");
        assertThat(saved.get("DESCANSO").getRole()).isNull();
        assertThat(saved.get("DESCANSO").getPeriod()).isNull();
    }

    @Test
    void savesNothingWhenRolesAndCodesAlreadyExist() {
        saveRoles();
        ShiftTestData.CATALOG_CODES.forEach(code -> codes.put(code, ShiftTestData.shiftCode(code)));

        initializer.run(null);

        verify(roleRepository, never()).save(any());
        verify(shiftCodeRepository, never()).save(any());
    }

    @Test
    void createsOnlyTheMissingCodes() {
        saveRoles();
        List.of("DIA", "NOCHE", "DESCANSO").forEach(code -> codes.put(code, ShiftTestData.shiftCode(code)));
        stubCodeSave();

        initializer.run(null);

        verify(roleRepository, never()).save(any());
        assertThat(savedCodes()).containsOnlyKeys(
                "DIA6", "9-6", "12-7", "6-9y5-9", "2-9pm", "6-9am", "2-6pm", "6-9y2-6");
    }

    private void saveRoles() {
        roles.put("TITULAR", ShiftTestData.titularRole());
        roles.put("APOYO", ShiftTestData.supportRole());
    }

    private void stubRoleSave() {
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            Role saved = new Role(UUID.randomUUID(), role.getName(), role.getDispenser());
            roles.put(saved.getName(), saved);
            return saved;
        });
    }

    private void stubCodeSave() {
        when(shiftCodeRepository.save(any(ShiftCode.class))).thenAnswer(invocation -> {
            ShiftCode saved = withId(invocation.getArgument(0));
            codes.put(saved.getCode(), saved);
            return saved;
        });
    }

    /** The codes saved during the run, as the map holds them (with the id the pairing points to). */
    private Map<String, ShiftCode> savedCodes() {
        ArgumentCaptor<ShiftCode> captor = ArgumentCaptor.forClass(ShiftCode.class);
        verify(shiftCodeRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream()
                .map(shiftCode -> codes.get(shiftCode.getCode()))
                .collect(Collectors.toMap(ShiftCode::getCode, Function.identity()));
    }

    /** The code as the database would return it: same data with a generated id. */
    private static ShiftCode withId(ShiftCode shiftCode) {
        return ShiftCode.builder()
                .id(UUID.randomUUID())
                .code(shiftCode.getCode())
                .name(shiftCode.getName())
                .role(shiftCode.getRole())
                .period(shiftCode.getPeriod())
                .startHour1(shiftCode.getStartHour1())
                .endHour1(shiftCode.getEndHour1())
                .startHour2(shiftCode.getStartHour2())
                .endHour2(shiftCode.getEndHour2())
                .requiresSupport(shiftCode.isRequiresSupport())
                .pairedWithId(shiftCode.getPairedWithId())
                .active(shiftCode.isActive())
                .build();
    }
}
