package com.edu.unicauca.gasstation.backend.sales.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.sales.api.dto.*;
import com.edu.unicauca.gasstation.backend.sales.domain.models.*;
import org.springframework.stereotype.Component;

@Component
public class SalesTargetApiMapper {
    public SalesTarget toDomain(CreateSalesTargetRequest request) {
        return new SalesTarget(new SalesTargetIdentity(new SalesTargetPeriod(request.year(), request.month()),
                request.category()), request.value(), request.unit());
    }

    public SalesTargetResponse toResponse(StoredSalesTarget stored) {
        var target = stored.target();
        var period = target.identity().period();
        return new SalesTargetResponse(stored.id(), period.year(), period.month(), target.identity().category(),
                target.value(), target.unit(), stored.createdAt());
    }
}
