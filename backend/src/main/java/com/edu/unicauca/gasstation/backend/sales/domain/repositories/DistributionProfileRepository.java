package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import com.edu.unicauca.gasstation.backend.sales.domain.calculation.DistributionProfile;
import java.time.Instant;
import java.util.Optional;

public interface DistributionProfileRepository {
    StoredProfile register(DistributionProfile profile);
    Optional<StoredProfile> findById(long id);
    record StoredProfile(long id, DistributionProfile profile, Instant createdAt) { }
}
