package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import static com.edu.unicauca.gasstation.backend.shifts.ShiftTestData.SUPPORT_ROLE_ID;
import static com.edu.unicauca.gasstation.backend.shifts.ShiftTestData.TITULAR_ROLE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.shifts.ShiftTestData;
import com.edu.unicauca.gasstation.backend.shifts.catalog.RoleInfo;
import com.edu.unicauca.gasstation.backend.shifts.catalog.ShiftCodeInfo;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.RoleRepository;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftCodeRepository;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftError;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShiftCatalogServiceTest {

    @Mock
    private ShiftCodeRepository shiftCodeRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private ShiftCatalogService shiftCatalogService;

    @Test
    void getByCodeThrowsWhenTheCodeDoesNotExist() {
        when(shiftCodeRepository.findByCode("XYZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shiftCatalogService.getByCode("XYZ"))
                .isInstanceOfSatisfying(ShiftException.class, ex -> assertThat(ex.error()).isEqualTo(ShiftError.SHIFT_CODE_NOT_FOUND))
                .hasMessage("No se encontró el código de turno 'XYZ'");
    }

    @Test
    void findShiftCodeIdByCodeReturnsTheIdOrEmpty() {
        when(shiftCodeRepository.findByCode("NOCHE")).thenReturn(Optional.of(ShiftTestData.shiftCode("NOCHE")));
        when(shiftCodeRepository.findByCode("XYZ")).thenReturn(Optional.empty());

        assertThat(shiftCatalogService.findShiftCodeIdByCode("NOCHE")).contains(ShiftTestData.shiftCodeId("NOCHE"));
        assertThat(shiftCatalogService.findShiftCodeIdByCode("XYZ")).isEmpty();
    }

    @Test
    void getActiveCatalogConvertsEachCodeAndLeavesDescansoWithoutRole() {
        when(shiftCodeRepository.findActive()).thenReturn(List.of(
                ShiftTestData.shiftCode("6-9y5-9"), ShiftTestData.shiftCode("DESCANSO")));

        List<ShiftCodeInfo> catalog = shiftCatalogService.getActiveCatalog();

        assertThat(catalog.get(0)).isEqualTo(new ShiftCodeInfo(ShiftTestData.shiftCodeId("6-9y5-9"), "6-9y5-9",
                "6-9am y 5-9pm", new RoleInfo(SUPPORT_ROLE_ID, "APOYO", 3), "DAY",
                List.of(new ShiftCodeInfo.Segment(6, 9), new ShiftCodeInfo.Segment(17, 21)), 7,
                false, ShiftTestData.shiftCodeId("9-6"), true));
        assertThat(catalog.get(1).role()).isNull();
        assertThat(catalog.get(1).period()).isNull();
        assertThat(catalog.get(1).segments()).isEmpty();
    }

    @Test
    void getRolesByIdsReturnsAMapAndLeavesOutUnknownIds() {
        List<UUID> ids = List.of(TITULAR_ROLE_ID, UUID.randomUUID());
        when(roleRepository.findAllByIds(ids)).thenReturn(List.of(ShiftTestData.titularRole()));

        assertThat(shiftCatalogService.getRolesByIds(ids))
                .containsOnlyKeys(TITULAR_ROLE_ID)
                .containsEntry(TITULAR_ROLE_ID, new RoleInfo(TITULAR_ROLE_ID, "TITULAR", 1));
    }
}
