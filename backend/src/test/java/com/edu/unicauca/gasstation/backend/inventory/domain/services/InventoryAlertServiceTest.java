package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyLevel;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelAutonomy;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.InventoryAlert;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.InventoryAlertRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidInventoryAlertException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InventoryAlertConflictException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InventoryAlertNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.edu.unicauca.gasstation.backend.inventory.AlertTestData.alert;
import static com.edu.unicauca.gasstation.backend.inventory.AlertTestData.autonomy;
import static com.edu.unicauca.gasstation.backend.inventory.FuelType.DIESEL;
import static com.edu.unicauca.gasstation.backend.inventory.FuelType.EXTRA;
import static com.edu.unicauca.gasstation.backend.inventory.FuelType.MOTOR;
import static com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyStatus.CALCULATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryAlertServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-05T15:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 10, 0);

    @Mock
    private InventoryAlertRepository repository;
    @Mock
    private FuelAutonomyService autonomyService;

    @Captor
    private ArgumentCaptor<InventoryAlert> alertCaptor;
    @Captor
    private ArgumentCaptor<Collection<AlertStatus>> statusesCaptor;

    private InventoryAlertService service;

    @BeforeEach
    void setUp() {
        service = new InventoryAlertService(repository, autonomyService, CLOCK);
    }

    // ---------- reconcile: open ----------

    @ParameterizedTest
    @EnumSource(value = AutonomyLevel.class, names = {"WARNING", "CRITICAL"})
    void opensAnActiveAlertWhenTheAutonomyReachesAThreshold(AutonomyLevel level) {
        givenAutonomies(autonomy(MOTOR, CALCULATED, level, "3.50"));
        when(repository.findOpen(AlertType.AUTONOMY, MOTOR)).thenReturn(Optional.empty());
        givenSaveEchoes();

        service.reconcile(DATE);

        InventoryAlert saved = savedAlert();
        assertThat(saved.getId()).isNull();
        assertThat(saved.getAlertType()).isEqualTo(AlertType.AUTONOMY);
        assertThat(saved.getFuelType()).isEqualTo(MOTOR);
        assertThat(saved.getLevel()).isEqualTo(level);
        assertThat(saved.getStatus()).isEqualTo(AlertStatus.ACTIVE);
        assertThat(saved.getAutonomyDays()).isEqualByComparingTo("3.5");
        assertThat(saved.getOpenedOn()).isEqualTo(DATE);
        assertThat(saved.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void doesNothingWhenTheLevelIsOkAndThereIsNoOpenAlert() {
        givenAutonomies(autonomy(MOTOR, CALCULATED, AutonomyLevel.OK, "9.00"));
        when(repository.findOpen(AlertType.AUTONOMY, MOTOR)).thenReturn(Optional.empty());

        service.reconcile(DATE);

        verify(repository, never()).save(any());
    }

    // ---------- reconcile: resolve ----------

    @Test
    void resolvesTheOpenAlertWhenTheLevelGoesBackToOk() {
        InventoryAlert open = alert(1L, MOTOR, AutonomyLevel.WARNING, AlertStatus.ATTENDED, "4.00", DATE.minusDays(1));
        givenAutonomies(autonomy(MOTOR, CALCULATED, AutonomyLevel.OK, "9.00"));
        when(repository.findOpen(AlertType.AUTONOMY, MOTOR)).thenReturn(Optional.of(open));
        givenSaveEchoes();

        service.reconcile(DATE);

        InventoryAlert saved = savedAlert();
        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(saved.getResolvedAt()).isEqualTo(NOW);
    }

    // ---------- reconcile: refresh ----------

    @Test
    void updatesTheDaysButKeepsTheStatusWhenTheLevelDoesNotChange() {
        InventoryAlert open = alert(1L, MOTOR, AutonomyLevel.WARNING, AlertStatus.READ, "4.00", DATE.minusDays(1));
        givenAutonomies(autonomy(MOTOR, CALCULATED, AutonomyLevel.WARNING, "3.50"));
        when(repository.findOpen(AlertType.AUTONOMY, MOTOR)).thenReturn(Optional.of(open));
        givenSaveEchoes();

        service.reconcile(DATE);

        InventoryAlert saved = savedAlert();
        assertThat(saved.getAutonomyDays()).isEqualByComparingTo("3.5");
        assertThat(saved.getStatus()).isEqualTo(AlertStatus.READ);
        assertThat(saved.getOpenedOn()).isEqualTo(DATE.minusDays(1));
    }

    @Test
    void escalatingToCriticalMakesTheAlertActiveAgainAndClearsTheAttention() {
        InventoryAlert open = alert(1L, MOTOR, AutonomyLevel.WARNING, AlertStatus.ATTENDED, "4.00", DATE.minusDays(1));
        givenAutonomies(autonomy(MOTOR, CALCULATED, AutonomyLevel.CRITICAL, "1.50"));
        when(repository.findOpen(AlertType.AUTONOMY, MOTOR)).thenReturn(Optional.of(open));
        givenSaveEchoes();

        service.reconcile(DATE);

        InventoryAlert saved = savedAlert();
        assertThat(saved.getLevel()).isEqualTo(AutonomyLevel.CRITICAL);
        assertThat(saved.getStatus()).isEqualTo(AlertStatus.ACTIVE);
        assertThat(saved.getReadAt()).isNull();
        assertThat(saved.getAttendedAt()).isNull();
        assertThat(saved.getAttendNote()).isNull();
    }

    @Test
    void goingDownFromCriticalToWarningKeepsTheStatus() {
        InventoryAlert open = alert(1L, MOTOR, AutonomyLevel.CRITICAL, AlertStatus.READ, "1.50", DATE.minusDays(1));
        givenAutonomies(autonomy(MOTOR, CALCULATED, AutonomyLevel.WARNING, "3.00"));
        when(repository.findOpen(AlertType.AUTONOMY, MOTOR)).thenReturn(Optional.of(open));
        givenSaveEchoes();

        service.reconcile(DATE);

        InventoryAlert saved = savedAlert();
        assertThat(saved.getLevel()).isEqualTo(AutonomyLevel.WARNING);
        assertThat(saved.getStatus()).isEqualTo(AlertStatus.READ);
    }

    @Test
    void doesNotSaveWhenNothingChanged() {
        InventoryAlert open = alert(1L, MOTOR, AutonomyLevel.WARNING, AlertStatus.ACTIVE, "3.50", DATE.minusDays(1));
        givenAutonomies(autonomy(MOTOR, CALCULATED, AutonomyLevel.WARNING, "3.5"));
        when(repository.findOpen(AlertType.AUTONOMY, MOTOR)).thenReturn(Optional.of(open));

        service.reconcile(DATE);

        verify(repository, never()).save(any());
    }

    // ---------- reconcile: no data ----------

    @Test
    void leavesEverythingAloneWhenTheAutonomyCannotBeEvaluated() {
        givenAutonomies(
                autonomy(MOTOR, AutonomyStatus.INCOMPLETE_MEASUREMENT, AutonomyLevel.NOT_EVALUATED, null),
                autonomy(DIESEL, AutonomyStatus.NO_SALES_HISTORY, AutonomyLevel.NOT_EVALUATED, null),
                autonomy(EXTRA, CALCULATED, AutonomyLevel.NOT_EVALUATED, "4.00"));

        service.reconcile(DATE);

        // Missing data is not proof that the tank recovered: not even the open alerts are read
        verifyNoInteractions(repository);
    }

    @Test
    void reconcilesEachFuelOnItsOwn() {
        InventoryAlert dieselOpen = alert(7L, DIESEL, AutonomyLevel.CRITICAL, AlertStatus.ACTIVE, "1.00", DATE.minusDays(1));
        givenAutonomies(
                autonomy(MOTOR, CALCULATED, AutonomyLevel.WARNING, "4.00"),
                autonomy(DIESEL, CALCULATED, AutonomyLevel.OK, "8.00"));
        when(repository.findOpen(AlertType.AUTONOMY, MOTOR)).thenReturn(Optional.empty());
        when(repository.findOpen(AlertType.AUTONOMY, DIESEL)).thenReturn(Optional.of(dieselOpen));
        givenSaveEchoes();

        service.reconcile(DATE);

        verify(repository, times(2)).save(alertCaptor.capture());
        assertThat(alertCaptor.getAllValues()).extracting(InventoryAlert::getFuelType, InventoryAlert::getStatus)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(MOTOR, AlertStatus.ACTIVE),
                        org.assertj.core.groups.Tuple.tuple(DIESEL, AlertStatus.RESOLVED));
    }

    @Test
    void reconcileRejectsAMissingDate() {
        assertThatThrownBy(() -> service.reconcile(null))
                .isInstanceOf(InvalidInventoryAlertException.class)
                .hasMessage("The journal date is required");
        verifyNoInteractions(repository, autonomyService);
    }

    // ---------- list and count ----------

    @Test
    void listsTheOpenAlertsByDefaultWithTheCriticalOnesFirst() {
        InventoryAlert warningNewest = alert(3L, MOTOR, AutonomyLevel.WARNING, AlertStatus.ACTIVE, "4.00", DATE);
        InventoryAlert criticalNewer = alert(2L, DIESEL, AutonomyLevel.CRITICAL, AlertStatus.READ, "1.00", DATE.minusDays(1));
        InventoryAlert criticalOlder = alert(1L, EXTRA, AutonomyLevel.CRITICAL, AlertStatus.ATTENDED, "0.50", DATE.minusDays(2));
        when(repository.findByStatuses(anyCollection())).thenReturn(List.of(warningNewest, criticalNewer, criticalOlder));

        List<InventoryAlert> result = service.findAll(null);

        verify(repository).findByStatuses(statusesCaptor.capture());
        assertThat(statusesCaptor.getValue())
                .containsExactlyInAnyOrder(AlertStatus.ACTIVE, AlertStatus.READ, AlertStatus.ATTENDED);
        assertThat(result).extracting(InventoryAlert::getId).containsExactly(2L, 1L, 3L);
    }

    @Test
    void listsOnlyTheRequestedStatus() {
        when(repository.findByStatuses(anyCollection())).thenReturn(List.of());

        service.findAll(AlertStatus.RESOLVED);

        verify(repository).findByStatuses(statusesCaptor.capture());
        assertThat(statusesCaptor.getValue()).containsExactly(AlertStatus.RESOLVED);
    }

    @Test
    void theBellCountsOnlyActiveAndReadAlerts() {
        when(repository.countByStatuses(List.of(AlertStatus.ACTIVE, AlertStatus.READ))).thenReturn(3L);

        assertThat(service.countPending()).isEqualTo(3L);
    }

    // ---------- mark as read ----------

    @Test
    void markReadMovesAnActiveAlertToRead() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                alert(1L, MOTOR, AutonomyLevel.WARNING, AlertStatus.ACTIVE, "4.00", DATE)));
        givenSaveEchoes();

        InventoryAlert result = service.markRead(1L);

        assertThat(result.getStatus()).isEqualTo(AlertStatus.READ);
        assertThat(result.getReadAt()).isEqualTo(NOW);
    }

    @ParameterizedTest
    @EnumSource(value = AlertStatus.class, names = {"READ", "ATTENDED", "RESOLVED"})
    void markReadDoesNotChangeAnAlertThatIsNotActive(AlertStatus status) {
        InventoryAlert existing = alert(1L, MOTOR, AutonomyLevel.WARNING, status, "4.00", DATE);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        InventoryAlert result = service.markRead(1L);

        assertThat(result.getStatus()).isEqualTo(status);
        verify(repository, never()).save(any());
    }

    @Test
    void markReadFailsForAnUnknownAlert() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(99L)).isInstanceOf(InventoryAlertNotFoundException.class);
    }

    // ---------- attend ----------

    @Test
    void attendingAnActiveAlertAlsoMarksItAsReadAndTrimsTheNote() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                alert(1L, MOTOR, AutonomyLevel.CRITICAL, AlertStatus.ACTIVE, "1.00", DATE)));
        givenSaveEchoes();

        InventoryAlert result = service.attend(1L, "  Fuel ordered  ");

        assertThat(result.getStatus()).isEqualTo(AlertStatus.ATTENDED);
        assertThat(result.getAttendedAt()).isEqualTo(NOW);
        assertThat(result.getReadAt()).isEqualTo(NOW);
        assertThat(result.getAttendNote()).isEqualTo("Fuel ordered");
    }

    @Test
    void attendingKeepsTheOriginalReadTime() {
        InventoryAlert read = alert(1L, MOTOR, AutonomyLevel.CRITICAL, AlertStatus.READ, "1.00", DATE);
        when(repository.findById(1L)).thenReturn(Optional.of(read));
        givenSaveEchoes();

        InventoryAlert result = service.attend(1L, null);

        assertThat(result.getReadAt()).isEqualTo(DATE.atTime(11, 0));
        assertThat(result.getAttendNote()).isNull();
    }

    @Test
    void aBlankNoteIsStoredAsNull() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                alert(1L, MOTOR, AutonomyLevel.CRITICAL, AlertStatus.ACTIVE, "1.00", DATE)));
        givenSaveEchoes();

        assertThat(service.attend(1L, "   ").getAttendNote()).isNull();
    }

    @Test
    void attendingAgainReplacesTheNote() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                alert(1L, MOTOR, AutonomyLevel.CRITICAL, AlertStatus.ATTENDED, "1.00", DATE)));
        givenSaveEchoes();

        assertThat(service.attend(1L, "New note").getAttendNote()).isEqualTo("New note");
    }

    @Test
    void attendRejectsANoteLongerThan200Characters() {
        assertThatThrownBy(() -> service.attend(1L, "x".repeat(201)))
                .isInstanceOf(InvalidInventoryAlertException.class)
                .hasMessage("The note cannot exceed 200 characters");
        verifyNoInteractions(repository);
    }

    @Test
    void attendRejectsAResolvedAlert() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                alert(1L, MOTOR, AutonomyLevel.CRITICAL, AlertStatus.RESOLVED, "1.00", DATE)));

        assertThatThrownBy(() -> service.attend(1L, "Late"))
                .isInstanceOf(InventoryAlertConflictException.class)
                .hasMessage("The alert is already resolved");
        verify(repository, never()).save(any());
    }

    @Test
    void attendFailsForAnUnknownAlert() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.attend(99L, null)).isInstanceOf(InventoryAlertNotFoundException.class);
    }

    // ---------- helpers ----------

    private void givenAutonomies(FuelAutonomy... autonomies) {
        when(autonomyService.calculate(DATE)).thenReturn(List.of(autonomies));
    }

    private void givenSaveEchoes() {
        when(repository.save(any(InventoryAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private InventoryAlert savedAlert() {
        verify(repository).save(alertCaptor.capture());
        return alertCaptor.getValue();
    }
}