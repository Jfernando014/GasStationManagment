package com.edu.unicauca.gasstation.backend.incentives.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.incentives.infrastructure.persistence.entities.IncentiveAdditiveSaleEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository used by {@code AdditiveSaleRepositoryImpl}. */
public interface JpaAdditiveSaleRepository extends JpaRepository<IncentiveAdditiveSaleEntity, Long> {

    List<IncentiveAdditiveSaleEntity> findByJournalDateOrderByVendorNameAscVendorDocumentAscSkuAsc(LocalDate journalDate);

    List<IncentiveAdditiveSaleEntity> findByJournalDateBetweenOrderByJournalDateAscVendorNameAscSkuAsc(
            LocalDate from, LocalDate to);

    List<IncentiveAdditiveSaleEntity> findByJournalDateBetweenAndVendorDocumentOrderByJournalDateAscSkuAsc(
            LocalDate from, LocalDate to, String vendorDocument);

    // Runs right away so the new rows of the date do not collide with the old ones on the unique index
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from IncentiveAdditiveSaleEntity sale where sale.journalDate = :journalDate")
    void deleteByJournalDate(@Param("journalDate") LocalDate journalDate);
}
