package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.sales.domain.models.DiscrepancyStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.models.VendorCloseProduct;
import com.edu.unicauca.gasstation.backend.sales.domain.models.VendorShiftClose;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.VendorCloseProductEntity;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.VendorShiftCloseEntity;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories.VendorShiftCloseJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VendorShiftCloseRepositoryAdapterTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 19);

    @Mock
    private VendorShiftCloseJpaRepository jpaRepository;

    @InjectMocks
    private VendorShiftCloseRepositoryAdapter adapter;

    @Test
    @DisplayName("saveAll updates existing entity in place when (journalDate, vendorDocument, shiftName) already exists")
    void updatesExistingRowInsteadOfInsertingDuplicate() {
        UUID existingId = UUID.randomUUID();
        VendorShiftCloseEntity existing = VendorShiftCloseEntity.builder()
                .id(existingId)
                .journalDate(DATE)
                .vendorDocument("52875796")
                .vendorName("Mónica Dorado")
                .shiftName("12-7")
                .pumpNumber(3)
                .cash(new BigDecimal("1000000.00"))
                .ga(BigDecimal.ZERO)
                .credibanco(BigDecimal.ZERO)
                .qr(BigDecimal.ZERO)
                .redemptions(BigDecimal.ZERO)
                .totalDelivered(new BigDecimal("1000000.00"))
                .gallonsMotor(BigDecimal.ZERO)
                .gallonsDiesel(BigDecimal.ZERO)
                .gallonsExtra(BigDecimal.ZERO)
                .gallonsMaxpro(BigDecimal.ZERO)
                .basketDominusAmount(BigDecimal.ZERO)
                .dominusTirilla(new BigDecimal("1000000.00"))
                .realDiscrepancy(BigDecimal.ZERO)
                .appliedTolerance(BigDecimal.ZERO)
                .resultStatus("CUADRA")
                .products(new ArrayList<>())
                .build();
        existing.addProduct(VendorCloseProductEntity.builder()
                .sku("76102").productName("Old").category("Aditivos")
                .unitsSold(1).unitPrice(new BigDecimal("6800.00")).totalAmount(new BigDecimal("6800.00"))
                .build());

        when(jpaRepository.findByJournalDateOrderByShiftNameAscPumpNumberAsc(DATE))
                .thenReturn(List.of(existing));
        when(jpaRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        VendorShiftClose incoming = new VendorShiftClose(
                null, DATE, "52875796", "Mónica Dorado", "12-7", 3,
                new BigDecimal("2373000.00"), BigDecimal.ZERO, new BigDecimal("1354436.00"),
                new BigDecimal("162606.00"), BigDecimal.ZERO, new BigDecimal("3890042.00"),
                new BigDecimal("223.701"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("20800.00"), new BigDecimal("3890028.87"), new BigDecimal("13.13"),
                new BigDecimal("0.50"), DiscrepancyStatus.SOBRA,
                List.of(new VendorCloseProduct(null, "75089", "Super Concentrado", "Aditivos",
                        2, new BigDecimal("7200.00"), new BigDecimal("14400.00")))
        );

        List<VendorShiftClose> saved = adapter.saveAll(List.of(incoming));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<VendorShiftCloseEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(jpaRepository).saveAllAndFlush(captor.capture());

        List<VendorShiftCloseEntity> persisted = captor.getValue();
        assertThat(persisted).hasSize(1);
        assertThat(persisted.get(0)).isSameAs(existing);
        assertThat(persisted.get(0).getId()).isEqualTo(existingId);
        assertThat(persisted.get(0).getCash()).isEqualByComparingTo("2373000.00");
        assertThat(persisted.get(0).getProducts()).hasSize(1);
        assertThat(persisted.get(0).getProducts().get(0).getSku()).isEqualTo("75089");
        assertThat(saved.get(0).id()).isEqualTo(existingId);
    }

    @Test
    @DisplayName("saveAll migrates an existing cédula close to the worker UUID when (vendorName, shiftName) match")
    void updatesLegacyCedulaRowWhenRealWorkerUuidArrives() {
        UUID existingId = UUID.randomUUID();
        String workerUuid = "8293f40b-5ecc-487c-a2fb-efd7aec4811c";
        VendorShiftCloseEntity existing = VendorShiftCloseEntity.builder()
                .id(existingId)
                .journalDate(DATE)
                .vendorDocument("52875796")
                .vendorName("Mónica Dorado")
                .shiftName("12-7")
                .pumpNumber(3)
                .cash(BigDecimal.ZERO)
                .ga(BigDecimal.ZERO)
                .credibanco(BigDecimal.ZERO)
                .qr(BigDecimal.ZERO)
                .redemptions(BigDecimal.ZERO)
                .totalDelivered(BigDecimal.ZERO)
                .gallonsMotor(BigDecimal.ZERO)
                .gallonsDiesel(BigDecimal.ZERO)
                .gallonsExtra(BigDecimal.ZERO)
                .gallonsMaxpro(BigDecimal.ZERO)
                .basketDominusAmount(BigDecimal.ZERO)
                .dominusTirilla(BigDecimal.ZERO)
                .realDiscrepancy(BigDecimal.ZERO)
                .appliedTolerance(BigDecimal.ZERO)
                .resultStatus("CUADRA")
                .products(new ArrayList<>())
                .build();

        when(jpaRepository.findByJournalDateOrderByShiftNameAscPumpNumberAsc(DATE))
                .thenReturn(List.of(existing));
        when(jpaRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        VendorShiftClose incoming = new VendorShiftClose(
                null, DATE, workerUuid, "Monica Dorado", "12-7", 3,
                new BigDecimal("2373000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("2373000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("2373000.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, DiscrepancyStatus.CUADRA, List.of()
        );

        List<VendorShiftClose> saved = adapter.saveAll(List.of(incoming));

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).id()).isEqualTo(existingId);
        assertThat(saved.get(0).vendorDocument()).isEqualTo(workerUuid);
    }

    @Test
    @DisplayName("saveAll deduplicates identical unique keys within the same batch")
    void deduplicatesBatchByUniqueKey() {
        when(jpaRepository.findByJournalDateOrderByShiftNameAscPumpNumberAsc(DATE))
                .thenReturn(List.of());
        when(jpaRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        VendorShiftClose first = new VendorShiftClose(
                null, DATE, "52875796", "Mónica Dorado", "12-7", 3,
                new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, DiscrepancyStatus.CUADRA, List.of());
        VendorShiftClose second = new VendorShiftClose(
                null, DATE, "52875796", "Mónica Dorado", "12-7", 3,
                new BigDecimal("200.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("200.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("200.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, DiscrepancyStatus.CUADRA, List.of());

        List<VendorShiftClose> saved = adapter.saveAll(List.of(first, second));

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).cash()).isEqualByComparingTo("200.00");
    }
}
