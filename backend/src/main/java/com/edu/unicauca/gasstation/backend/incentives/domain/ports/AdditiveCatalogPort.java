package com.edu.unicauca.gasstation.backend.incentives.domain.ports;

import java.util.List;
import java.util.Optional;

/** Additive catalog, to be served by inventory. Until then a temporary in-memory one is used. */
public interface AdditiveCatalogPort {

    /** Any product, active or not. */
    Optional<AdditiveInfo> findById(Long productId);

    /** Any product, active or not, by the code that sales reports as SKU; surrounding spaces are ignored. */
    Optional<AdditiveInfo> findByCode(String code);

    /** Active additives ordered by name. */
    List<AdditiveInfo> listActiveAdditives();
}
