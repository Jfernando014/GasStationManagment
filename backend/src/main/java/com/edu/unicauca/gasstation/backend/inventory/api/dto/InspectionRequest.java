package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.InspectionCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class InspectionRequest {

    public static final String CODE_REQUIRED = "The inspection code is required";
    public static final String CODE_SIZE = "The code cannot exceed 20 characters";
    public static final String CATEGORY_REQUIRED = "The category is required";
    public static final String TARGET_REQUIRED = "The inspected equipment or tank is required";
    public static final String TARGET_SIZE = "The target cannot exceed 150 characters";
    public static final String DATE_EXECUTED_REQUIRED = "The execution date is required";
    public static final String DATE_DUE_REQUIRED = "The due date is required";
    public static final String CERTIFIER_REQUIRED = "The certifier is required";
    public static final String CERTIFIER_SIZE = "The certifier cannot exceed 150 characters";
    public static final String TOLERANCE_REQUIRED = "The measured tolerance is required";
    public static final String TOLERANCE_SIZE = "The tolerance cannot exceed 100 characters";
    public static final String CERT_DOC_SIZE = "The certificate document cannot exceed 200 characters";

    @Schema(description = "Code of the inspection record", example = "CAL-ONAC-2026-08")
    @NotBlank(message = CODE_REQUIRED)
    @Size(max = 20, message = CODE_SIZE)
    private String code;

    @Schema(description = "Regulatory category", example = "ONAC")
    @NotNull(message = CATEGORY_REQUIRED)
    private InspectionCategory category;

    @Schema(description = "Inspected equipment or tank", example = "Dispensador 1 y 2")
    @NotBlank(message = TARGET_REQUIRED)
    @Size(max = 150, message = TARGET_SIZE)
    private String target;

    @Schema(description = "Date on which the inspection was executed", example = "2026-03-15")
    @NotNull(message = DATE_EXECUTED_REQUIRED)
    private LocalDate dateExecuted;

    @Schema(description = "Date on which the certification expires", example = "2026-09-15")
    @NotNull(message = DATE_DUE_REQUIRED)
    private LocalDate dateDue;

    @Schema(description = "Accredited laboratory or entity", example = "Metrología del Cauca SAS")
    @NotBlank(message = CERTIFIER_REQUIRED)
    @Size(max = 150, message = CERTIFIER_SIZE)
    private String certifier;

    @Schema(description = "Measured tolerance or result", example = "+3,8 mL (Límite ±15 mL)")
    @NotBlank(message = TOLERANCE_REQUIRED)
    @Size(max = 100, message = TOLERANCE_SIZE)
    private String toleranceMeasured;

    @Schema(description = "File name of the certificate, when it is registered", example = "CERT-ONAC-0812.pdf", nullable = true)
    @Size(max = 200, message = CERT_DOC_SIZE)
    private String certDoc;
}
