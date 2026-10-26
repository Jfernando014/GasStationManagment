package com.edu.unicauca.gasstation.backend.shifts.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.shifts.api.dtos.RotationDtos.RotationPreviewResponse;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.RotationDtos.RotationResultResponse;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.RotationPreview;
import org.mapstruct.Mapper;

/**
 * Converts the results of the automatic rotation into the DTOs of the REST API. The records have the same shape;
 * the enums travel as text.
 */
@Mapper(componentModel = "spring")
public interface ShiftRotationMapper {

    RotationPreviewResponse toResponse(RotationPreview preview);

    RotationResultResponse toResponse(RotationPreview.Outcome outcome);
}
