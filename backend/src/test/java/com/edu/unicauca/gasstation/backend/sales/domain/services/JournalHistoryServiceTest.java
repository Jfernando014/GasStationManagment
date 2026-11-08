package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.JournalCalendarDay;
import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.JournalHistoryCalendar;
import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.JournalHistoryDayDetail;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.ClosingInventorySnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.InventoryAlertSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JournalHistoryService - Monthly Calendar & Migrated Days (C-10, S3-06)")
class JournalHistoryServiceTest {

    @Mock
    private JournalRepository journalRepository;

    @Mock
    private SellerCloseQuery sellerCloseQuery;

    @Mock
    private InventoryClosingReadPort inventoryClosingReadPort;

    @InjectMocks
    private JournalHistoryService journalHistoryService;

    @Test
    @DisplayName("August 2026 calendar shows migrated days, closed system journals and discrepancy/alert markers")
    void buildsAugust2026CalendarWithMigratedAndClosedDays() {
        LocalDate aug5 = LocalDate.of(2026, 8, 5);
        Journal closedAug5 = Journal.builder()
                .id(5L)
                .date(aug5)
                .consecutive(293L)
                .status(JournalStatus.CLOSED)
                .build();

        when(journalRepository.findByDateBetweenOrderByDateAsc(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of(closedAug5));
        when(sellerCloseQuery.findVendorClosesByDate(any())).thenReturn(List.of());
        when(inventoryClosingReadPort.findClosingSnapshot(any())).thenAnswer(inv -> {
            LocalDate date = inv.getArgument(0);
            if (aug5.equals(date)) {
                return new ClosingInventorySnapshot(
                        true,
                        List.of(),
                        List.of(new InventoryAlertSummary(1L, "AUTONOMY", "DIESEL", "CRITICAL", "ACTIVE", new BigDecimal("1.2"), aug5))
                );
            }
            return ClosingInventorySnapshot.empty();
        });

        JournalHistoryCalendar calendar = journalHistoryService.getMonthCalendar(YearMonth.of(2026, 8));

        assertThat(calendar.yearMonth()).isEqualTo("2026-08");
        assertThat(calendar.days()).hasSize(31);
        assertThat(calendar.closedInSystemCount()).isEqualTo(1);
        // Days 1..28 except day 5 = 27 migrated days
        assertThat(calendar.migratedDaysCount()).isEqualTo(27);
        assertThat(calendar.daysWithIssuesCount()).isGreaterThan(0);

        JournalCalendarDay day1 = calendar.days().get(0);
        assertThat(day1.date()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(day1.type()).isEqualTo("MIGRATED");
        assertThat(day1.status()).isEqualTo("CLOSED");
        assertThat(day1.hasDiscrepancy()).isTrue();
        assertThat(day1.markers()).contains("descuadre");

        JournalCalendarDay day5 = calendar.days().get(4);
        assertThat(day5.date()).isEqualTo(aug5);
        assertThat(day5.type()).isEqualTo("JOURNAL");
        assertThat(day5.status()).isEqualTo("CLOSED");
        assertThat(day5.hasAlert()).isTrue();
        assertThat(day5.markers()).contains("alerta");
    }

    @Test
    @DisplayName("getDayDetail returns migrated vendor sales, electronic totals, variations and cash balance for August 1")
    void returnsMigratedDayDetailForAugustFirst() {
        LocalDate aug1 = LocalDate.of(2026, 8, 1);
        when(journalRepository.findByDate(aug1)).thenReturn(Optional.empty());
        when(sellerCloseQuery.findVendorClosesByDate(aug1)).thenReturn(List.of());
        when(sellerCloseQuery.findDaySummary(aug1)).thenReturn(Optional.empty());
        when(inventoryClosingReadPort.findClosingSnapshot(aug1)).thenReturn(ClosingInventorySnapshot.empty());

        JournalHistoryDayDetail detail = journalHistoryService.getDayDetail(aug1);

        assertThat(detail.date()).isEqualTo(aug1);
        assertThat(detail.type()).isEqualTo("MIGRATED");
        assertThat(detail.status()).isEqualTo("CLOSED");
        assertThat(detail.hasDiscrepancy()).isTrue();
        assertThat(detail.vendorSales()).hasSize(3);
        assertThat(detail.electronicSummary()).isNotNull();
        assertThat(detail.variations()).isNotEmpty();
        assertThat(detail.cashBalance()).isNotNull();
        assertThat(detail.cashBalance().planilla()).isEqualTo(289L);
    }
}
