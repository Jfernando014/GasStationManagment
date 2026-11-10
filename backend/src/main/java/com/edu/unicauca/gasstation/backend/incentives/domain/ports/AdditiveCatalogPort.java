package com.edu.unicauca.gasstation.backend.incentives.domain.ports;

import java.util.List;
import java.util.Optional;

/** Additive catalog, served by inventory. Only products of the additive category are found. */
public interface AdditiveCatalogPort {

    /** Any additive, active or not. */
    Optional<AdditiveInfo> findById(Long productId);

    /** Any additive, active or not, by the code that sales reports as SKU; surrounding spaces are ignored. */
    Optional<AdditiveInfo> findByCode(String code);

    /** Active additives ordered by name. */
    List<AdditiveInfo> listActiveAdditives();
}
