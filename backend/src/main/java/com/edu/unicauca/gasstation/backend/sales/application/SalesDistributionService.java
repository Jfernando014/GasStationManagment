package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.sales.domain.calculation.*;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DistributionProfileRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DistributionProfileRepository.StoredProfile;
import com.edu.unicauca.gasstation.backend.sales.domain.services.SalesTargetService;
import com.edu.unicauca.gasstation.backend.sales.exception.SalesTargetNotFoundException;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.adapters.PublicShiftCoverageResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class SalesDistributionService {
    private final DistributionProfileRepository profiles;
    private final SalesTargetService targets;
    private final PublicShiftCoverageResolver coverage;
    @Transactional
    public StoredProfile register(DistributionProfile profile) { return profiles.register(profile); }
    public StoredProfile findById(long id) { return profiles.findById(id).orElseThrow(SalesTargetNotFoundException::new); }
    public DistributionCalculation.Result calculate(long targetId, long profileId, int outputScale,
                                                    List<DistributionCalculation.Assignment> assignments) {
        var target=targets.findById(targetId).orElseThrow(SalesTargetNotFoundException::new);
        var profile=findById(profileId);
        return new SalesTargetDistributionEngine().calculate(new DistributionCalculation.Input(target.target(),profile.profile(),
                coverage.resolve(assignments),outputScale));
    }
}
