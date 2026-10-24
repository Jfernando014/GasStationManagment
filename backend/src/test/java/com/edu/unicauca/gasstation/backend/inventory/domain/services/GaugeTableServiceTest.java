package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.TankTestData;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.GaugeTable;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.GaugeTableRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.DuplicateGaugeTableNameException;
import com.edu.unicauca.gasstation.backend.inventory.exception.GaugeTableNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.exception.HeightExceedsGaugeException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidGaugeTableException;
import com.edu.unicauca.gasstation.backend.inventory.exception.TankNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.exception.TankWithoutGaugeTableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.edu.unicauca.gasstation.backend.inventory.GaugeTestData.curve;
import static com.edu.unicauca.gasstation.backend.inventory.GaugeTestData.point;
import static com.edu.unicauca.gasstation.backend.inventory.GaugeTestData.table;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GaugeTableServiceTest {

    private static final BigDecimal DEAD_VOLUME = new BigDecimal("20");

    @Mock
    private GaugeTableRepository gaugeTableRepository;

    @Mock
    private TankRepository tankRepository;

    private GaugeTableService gaugeTableService;

    @BeforeEach
    void setUp() {
        // @Value no se resuelve con @InjectMocks: el servicio se construye a mano
        gaugeTableService = new GaugeTableService(gaugeTableRepository, tankRepository, new GaugeCsvParser(), DEAD_VOLUME);
    }

    // ---------- create ----------

    @Test
    void createTrimsTheNameValidatesAndSaves() {
        GaugeTable table = curve(null, "  Tanques 1 y 2  ");
        when(gaugeTableRepository.existsByName("Tanques 1 y 2")).thenReturn(false);
        when(gaugeTableRepository.save(table)).thenReturn(table);

        GaugeTable created = gaugeTableService.create(table);

        assertThat(created.getName()).isEqualTo("Tanques 1 y 2");
        verify(gaugeTableRepository).save(table);
    }

    @Test
    void createRejectsInvalidPointsWithoutTouchingTheRepository() {
        GaugeTable table = table(null, "Curva", point("5", "16"), point("1", "1"));

        assertThatThrownBy(() -> gaugeTableService.create(table)).isInstanceOf(InvalidGaugeTableException.class);
        verifyNoInteractions(gaugeTableRepository);
    }

    @Test
    void createThrowsWhenTheNameAlreadyExists() {
        GaugeTable table = curve(null, "Tanques 1 y 2");
        when(gaugeTableRepository.existsByName("Tanques 1 y 2")).thenReturn(true);

        assertThatThrownBy(() -> gaugeTableService.create(table))
                .isInstanceOf(DuplicateGaugeTableNameException.class)
                .hasMessage(new DuplicateGaugeTableNameException("Tanques 1 y 2").getMessage());
        verify(gaugeTableRepository, never()).save(any());
    }

    @Test
    void createFromCsvParsesAndSavesTheTable() {
        when(gaugeTableRepository.existsByName("Tanques 3 y 4")).thenReturn(false);
        when(gaugeTableRepository.save(any(GaugeTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GaugeTable created = gaugeTableService.createFromCsv("Tanques 3 y 4", "Altura_cm,Volumen_Galones\n1,1\n2,4\n");

        assertThat(created.getName()).isEqualTo("Tanques 3 y 4");
        assertThat(created.getPoints()).hasSize(2);
    }

    @Test
    void createFromCsvRejectsAnUnorderedFile() {
        assertThatThrownBy(() -> gaugeTableService.createFromCsv("Tanques 3 y 4", "5,16\n1,1"))
                .isInstanceOf(InvalidGaugeTableException.class);
        verify(gaugeTableRepository, never()).save(any());
    }

    // ---------- update ----------

    @Test
    void updateReplacesTheNameAndAllThePoints() {
        GaugeTable current = curve(1L, "Anterior");
        GaugeTable changes = table(null, "  Nuevo ", point("1", "1"), point("2", "4"));
        when(gaugeTableRepository.findById(1L)).thenReturn(Optional.of(current));
        when(gaugeTableRepository.existsByNameAndIdNot("Nuevo", 1L)).thenReturn(false);
        when(gaugeTableRepository.save(current)).thenReturn(current);

        GaugeTable updated = gaugeTableService.update(1L, changes);

        assertThat(updated.getId()).isEqualTo(1L);
        assertThat(updated.getName()).isEqualTo("Nuevo");
        assertThat(updated.getPoints()).hasSize(2);
    }

    @Test
    void updateThrowsWhenTheTableDoesNotExist() {
        when(gaugeTableRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gaugeTableService.update(99L, curve(null, "Curva")))
                .isInstanceOf(GaugeTableNotFoundException.class);
        verify(gaugeTableRepository, never()).save(any());
    }

    @Test
    void updateThrowsWhenAnotherTableUsesTheName() {
        when(gaugeTableRepository.findById(1L)).thenReturn(Optional.of(curve(1L, "Anterior")));
        when(gaugeTableRepository.existsByNameAndIdNot("Ocupado", 1L)).thenReturn(true);

        assertThatThrownBy(() -> gaugeTableService.update(1L, curve(null, "Ocupado")))
                .isInstanceOf(DuplicateGaugeTableNameException.class);
        verify(gaugeTableRepository, never()).save(any());
    }

    @Test
    void updateRejectsInvalidPointsAndKeepsTheStoredTable() {
        GaugeTable current = curve(1L, "Anterior");
        GaugeTable changes = table(null, "Nuevo", point("1", "10"), point("2", "9"));
        when(gaugeTableRepository.findById(1L)).thenReturn(Optional.of(current));

        assertThatThrownBy(() -> gaugeTableService.update(1L, changes)).isInstanceOf(InvalidGaugeTableException.class);
        verify(gaugeTableRepository, never()).save(any());
        assertThat(current.getName()).isEqualTo("Anterior");
        assertThat(current.getPoints()).hasSize(7);
    }

    @Test
    void replacePointsFromCsvKeepsTheNameAndReplacesThePoints() {
        GaugeTable current = curve(1L, "Tanques 1 y 2");
        when(gaugeTableRepository.findById(1L)).thenReturn(Optional.of(current));
        when(gaugeTableRepository.existsByNameAndIdNot("Tanques 1 y 2", 1L)).thenReturn(false);
        when(gaugeTableRepository.save(current)).thenReturn(current);

        GaugeTable updated = gaugeTableService.replacePointsFromCsv(1L, "1,1\n2,4\n3,7\n");

        assertThat(updated.getName()).isEqualTo("Tanques 1 y 2");
        assertThat(updated.getPoints()).hasSize(3);
    }

    @Test
    void replacePointsFromCsvRejectsAnUnorderedFile() {
        when(gaugeTableRepository.findById(1L)).thenReturn(Optional.of(curve(1L, "Tanques 1 y 2")));

        assertThatThrownBy(() -> gaugeTableService.replacePointsFromCsv(1L, "5,16\n1,1"))
                .isInstanceOf(InvalidGaugeTableException.class);
        verify(gaugeTableRepository, never()).save(any());
    }

    // ---------- consultas ----------

    @Test
    void findByIdReturnsTheTable() {
        GaugeTable table = curve(1L, "Curva");
        when(gaugeTableRepository.findById(1L)).thenReturn(Optional.of(table));

        assertThat(gaugeTableService.findById(1L)).isSameAs(table);
    }

    @Test
    void findByIdThrowsWhenTheTableDoesNotExist() {
        when(gaugeTableRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gaugeTableService.findById(99L))
                .isInstanceOf(GaugeTableNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findAllReturnsTheTables() {
        GaugeTable first = curve(1L, "A");
        when(gaugeTableRepository.findAllOrderByName()).thenReturn(List.of(first));

        assertThat(gaugeTableService.findAll()).containsExactly(first);
    }

    @Test
    void findTanksUsingReturnsTheTanksOfTheTable() {
        Tank tank = TankTestData.tank(1L, "T1C1", "Tanque 1 Comp. 1");
        when(tankRepository.findByGaugeTableId(10L)).thenReturn(List.of(tank));

        assertThat(gaugeTableService.findTanksUsing(10L)).containsExactly(tank);
    }

    @Test
    void toCsvWritesTheTable() {
        assertThat(gaugeTableService.toCsv(curve(1L, "Curva"))).startsWith("Altura_cm,Volumen_Galones\n1,1\n");
    }

    // ---------- calculateVolume ----------

    @Test
    void calculateVolumeSubtractsTheDeadVolumeFromTheCertifiedCurve() {
        stubTankWithCurve();

        // T1C1 a 55 cm: curva 559 gal - 20 = 539 gal (criterio de aceptación de S1-28)
        assertThat(gaugeTableService.calculateVolume(1L, new BigDecimal("55"))).isEqualByComparingTo("539");
    }

    @Test
    void calculateVolumeInterpolatesBeforeSubtracting() {
        stubTankWithCurve();

        assertThat(gaugeTableService.calculateVolume(1L, new BigDecimal("55.5"))).isEqualByComparingTo("546.5");
    }

    @Test
    void calculateVolumeNeverGoesBelowZero() {
        stubTankWithCurve();

        // a 5 cm la curva da 16 gal: 16 - 20 sería negativo
        assertThat(gaugeTableService.calculateVolume(1L, new BigDecimal("5"))).isEqualByComparingTo("0");
    }

    @Test
    void calculateVolumeWithoutDeadVolumeReturnsTheCurveAsIs() {
        GaugeTableService withoutDeadVolume =
                new GaugeTableService(gaugeTableRepository, tankRepository, new GaugeCsvParser(), BigDecimal.ZERO);
        stubTankWithCurve();

        assertThat(withoutDeadVolume.calculateVolume(1L, new BigDecimal("55"))).isEqualByComparingTo("559");
    }

    @Test
    void calculateVolumeThrowsWhenTheHeightExceedsTheTable() {
        stubTankWithCurve();

        assertThatThrownBy(() -> gaugeTableService.calculateVolume(1L, new BigDecimal("239")))
                .isInstanceOf(HeightExceedsGaugeException.class);
    }

    @Test
    void calculateVolumeThrowsWhenTheTankDoesNotExist() {
        when(tankRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gaugeTableService.calculateVolume(99L, new BigDecimal("55")))
                .isInstanceOf(TankNotFoundException.class);
    }

    @Test
    void calculateVolumeThrowsWhenTheTankHasNoGaugeTable() {
        Tank tank = TankTestData.tank(1L, "T1C1", "Tanque 1 Comp. 1");
        tank.setGaugeTableId(null);
        when(tankRepository.findById(1L)).thenReturn(Optional.of(tank));

        assertThatThrownBy(() -> gaugeTableService.calculateVolume(1L, new BigDecimal("55")))
                .isInstanceOf(TankWithoutGaugeTableException.class)
                .hasMessage(new TankWithoutGaugeTableException(1L).getMessage());
        verifyNoInteractions(gaugeTableRepository);
    }

    @Test
    void calculateVolumeThrowsWhenTheReferencedTableIsMissing() {
        Tank tank = TankTestData.tank(1L, "T1C1", "Tanque 1 Comp. 1");
        tank.setGaugeTableId(10L);
        when(tankRepository.findById(1L)).thenReturn(Optional.of(tank));
        when(gaugeTableRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gaugeTableService.calculateVolume(1L, new BigDecimal("55")))
                .isInstanceOf(GaugeTableNotFoundException.class);
    }

    private void stubTankWithCurve() {
        Tank tank = TankTestData.tank(1L, "T1C1", "Tanque 1 Comp. 1");
        tank.setGaugeTableId(10L);
        when(tankRepository.findById(1L)).thenReturn(Optional.of(tank));
        when(gaugeTableRepository.findById(10L)).thenReturn(Optional.of(curve(10L, "Tanques 1 y 2")));
    }
    @Test
    void calculateVolumeOfALoadedTableSubtractsTheDeadVolume() {
        assertThat(gaugeTableService.calculateVolume(curve(10L, "Curva"), new BigDecimal("55")))
                .isEqualByComparingTo("539");
    }

    @Test
    void calculateVolumeOfALoadedTableNeverGoesBelowZero() {
        assertThat(gaugeTableService.calculateVolume(curve(10L, "Curva"), new BigDecimal("5")))
                .isEqualByComparingTo("0");
    }
}