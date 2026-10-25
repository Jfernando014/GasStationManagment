package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.VendorShiftCloseEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorShiftCloseJpaRepository extends JpaRepository<VendorShiftCloseEntity, UUID> {

    @EntityGraph(attributePaths = {"products"})
    List<VendorShiftCloseEntity> findByJournalDateOrderByShiftNameAscPumpNumberAsc(LocalDate journalDate);

    @EntityGraph(attributePaths = {"products"})
    Optional<VendorShiftCloseEntity> findByJournalDateAndVendorDocument(LocalDate journalDate, String vendorDocument);

    boolean existsByJournalDate(LocalDate journalDate);
}
