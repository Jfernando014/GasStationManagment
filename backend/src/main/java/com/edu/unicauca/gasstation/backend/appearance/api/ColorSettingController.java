package com.edu.unicauca.gasstation.backend.appearance.api;

import com.edu.unicauca.gasstation.backend.appearance.api.dtos.ColorSettingDtos.ColorThemeResponse;
import com.edu.unicauca.gasstation.backend.appearance.api.dtos.ColorSettingDtos.UpdateColorThemeRequest;
import com.edu.unicauca.gasstation.backend.appearance.api.dtos.ColorSettingDtos.UpdateSingleColorRequest;
import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorCategory;
import com.edu.unicauca.gasstation.backend.appearance.domain.services.ColorSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for editable UI colors of fuels, shift families, and individual sellers (SR-09).
 * Reading requires an authenticated session; writes and resets require {@code ADMIN}.
 */
@Tag(name = "Color settings", description = "Personalización editable de colores para combustibles, familias de "
        + "turno y vendedores. Las claves no modificadas heredan la paleta predeterminada del sistema.")
@RestController
@RequestMapping("/api/v1/color-settings")
@RequiredArgsConstructor
public class ColorSettingController {

    private final ColorSettingService service;

    @Operation(summary = "Consultar la configuración de colores vigente",
            description = "Retorna los colores resueltos de combustibles, familias de turno, vendedores personalizados "
                    + "y la paleta curada sugerida.")
    @ApiResponse(responseCode = "200", description = "Configuración actual de colores")
    @GetMapping
    public ColorThemeResponse getTheme() {
        return ColorThemeResponse.from(service.getTheme());
    }

    @Operation(summary = "Actualizar múltiples colores en un solo lote",
            description = "Solo ADMIN. Valida formato hexadecimal (#RRGGBB) y contraste seguro antes de guardar. "
                    + "Enviar colorHex nulo o vacío restablece esa clave a su valor predeterminado.")
    @ApiResponse(responseCode = "200", description = "Configuración actualizada")
    @ApiResponse(responseCode = "400", description = "Color inválido, contraste insuficiente o clave desconocida",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador")
    @PutMapping
    public ColorThemeResponse updateBatch(
            @Valid @RequestBody UpdateColorThemeRequest request,
            Authentication authentication) {
        AppearanceAdminAuthorization.requireAdmin(authentication);
        var commands = request.items().stream().map(item -> item.toCommand()).toList();
        return ColorThemeResponse.from(service.updateBatch(commands));
    }

    @Operation(summary = "Actualizar el color de una entidad específica",
            description = "Solo ADMIN. Aplica a un combustible (FUEL), familia de turno (SHIFT) o vendedor (WORKER).")
    @ApiResponse(responseCode = "200", description = "Configuración actualizada")
    @ApiResponse(responseCode = "400", description = "Color inválido, contraste insuficiente o clave desconocida",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador")
    @PutMapping("/{category}/{entityKey}")
    public ColorThemeResponse updateSingle(
            @Parameter(description = "Categoría: FUEL, SHIFT o WORKER", example = "FUEL")
            @PathVariable ColorCategory category,
            @Parameter(description = "Clave del combustible, turno o UUID del vendedor", example = "MOTOR")
            @PathVariable String entityKey,
            @Valid @RequestBody UpdateSingleColorRequest request,
            Authentication authentication) {
        AppearanceAdminAuthorization.requireAdmin(authentication);
        return ColorThemeResponse.from(service.upsertSingle(category, entityKey, request.colorHex()));
    }

    @Operation(summary = "Restablecer el color de una entidad específica",
            description = "Solo ADMIN. Elimina la personalización de la clave indicada para volver al color base.")
    @ApiResponse(responseCode = "200", description = "Configuración actualizada")
    @ApiResponse(responseCode = "400", description = "Clave desconocida",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador")
    @DeleteMapping("/{category}/{entityKey}")
    public ColorThemeResponse resetSingle(
            @PathVariable ColorCategory category,
            @PathVariable String entityKey,
            Authentication authentication) {
        AppearanceAdminAuthorization.requireAdmin(authentication);
        return ColorThemeResponse.from(service.resetSingle(category, entityKey));
    }

    @Operation(summary = "Restablecer colores predeterminados",
            description = "Solo ADMIN. Si se indica category (FUEL, SHIFT o WORKER), restablece solo ese grupo; "
                    + "de lo contrario restablece toda la personalización de colores.")
    @ApiResponse(responseCode = "200", description = "Configuración restablecida")
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador")
    @DeleteMapping
    public ColorThemeResponse resetAll(
            @Parameter(description = "Categoría opcional a restablecer (FUEL, SHIFT o WORKER)")
            @RequestParam(required = false) ColorCategory category,
            Authentication authentication) {
        AppearanceAdminAuthorization.requireAdmin(authentication);
        return ColorThemeResponse.from(service.resetAll(category));
    }
}
