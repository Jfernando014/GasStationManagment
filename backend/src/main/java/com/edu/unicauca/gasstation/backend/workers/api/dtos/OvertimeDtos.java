package com.edu.unicauca.gasstation.backend.workers.api.dtos;

import com.edu.unicauca.gasstation.backend.workers.domain.models.AdjustmentDirection;
import com.edu.unicauca.gasstation.backend.workers.domain.models.AffectedDate;
import com.edu.unicauca.gasstation.backend.workers.domain.models.CategoryHours;
import com.edu.unicauca.gasstation.backend.workers.domain.models.FixedHedoLine;
import com.edu.unicauca.gasstation.backend.workers.domain.models.Fortnight;
import com.edu.unicauca.gasstation.backend.workers.domain.models.IncompleteReason;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeAdjustment;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeCategory;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeDay;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeParameterOverview;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeParameterVersion;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeParameters;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeSheet;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** Requests and responses of the overtime sheet API. */
public final class OvertimeDtos {

    private OvertimeDtos() {
    }

    @Schema(description = "Horas de cada categoría. RDF: diurnas de domingo o festivo; RNO: nocturnas ordinarias; "
            + "RNF: nocturnas de domingo o festivo. HEDO, HENO, HEDF y HENF no se calculan: solo cambian por la línea "
            + "de HEDO fijas (HEDO) y por los ajustes")
    public record CategoryHoursResponse(
            @Schema(example = "13") int rdf,
            @Schema(example = "40") int rno,
            @Schema(example = "20") int rnf,
            @Schema(example = "0") int hedo,
            @Schema(example = "0") int heno,
            @Schema(example = "0") int hedf,
            @Schema(example = "0") int henf) {

        static CategoryHoursResponse from(CategoryHours hours) {
            return new CategoryHoursResponse(hours.rdf(), hours.rno(), hours.rnf(), hours.hedo(), hours.heno(),
                    hours.hedf(), hours.henf());
        }
    }

    @Schema(description = "Quincena: la 1 va del día 1 al 15 y la 2 del 16 al último día del mes")
    public record FortnightResponse(
            @Schema(description = "Mes (yyyy-MM)", example = "2026-08") String month,
            @Schema(description = "1 o 2", example = "1") int fortnight,
            @Schema(description = "Primer día", example = "2026-08-01") LocalDate from,
            @Schema(description = "Último día", example = "2026-08-15") LocalDate to) {

        static FortnightResponse from(Fortnight fortnight) {
            return new FortnightResponse(fortnight.month().toString(), fortnight.number(), fortnight.from(),
                    fortnight.to());
        }
    }

    @Schema(description = "Trabajador de la sábana")
    public record SheetWorkerResponse(
            @Schema(description = "Id del trabajador") UUID workerId,
            @Schema(description = "Nombre", example = "Trabajador de prueba Uno") String workerName,
            @Schema(description = "Rol del trabajador; nulo si Turnos no lo conoce", example = "TITULAR",
                    nullable = true) String role) {
    }

    @Schema(description = "Un día de la quincena. Las horas son las del turno que empezó ese día, aunque termine a la "
            + "madrugada siguiente, clasificadas con los parámetros vigentes ese día")
    public record DayResponse(
            @Schema(description = "Fecha operativa", example = "2026-08-07") LocalDate date,
            @Schema(description = "Día de la semana", example = "FRIDAY") DayOfWeek dayOfWeek,
            @Schema(description = "Verdadero si es domingo o festivo", example = "true") boolean holiday,
            @Schema(description = "Código de turno trabajado; nulo si descansó o el día no tiene registro",
                    example = "DIA", nullable = true) String workedCode,
            @Schema(description = "Horas guardadas del día, sin descontar el descanso", example = "12")
            int grossHours,
            @Schema(example = "11") int rdf,
            @Schema(example = "0") int rno,
            @Schema(example = "0") int rnf,
            @Schema(example = "0") int hedo,
            @Schema(example = "0") int heno,
            @Schema(example = "0") int hedf,
            @Schema(example = "0") int henf,
            @Schema(description = "Inicio de la versión de parámetros usada para este día", example = "2025-01-01")
            LocalDate parametersValidFrom) {

        static DayResponse from(OvertimeDay day) {
            CategoryHours hours = day.hours();
            return new DayResponse(day.date(), day.dayOfWeek(), day.holiday(), day.workedCode(), day.grossHours(),
                    hours.rdf(), hours.rno(), hours.rnf(), hours.hedo(), hours.heno(), hours.hedf(), hours.henf(),
                    day.parametersValidFrom());
        }
    }

    @Schema(description = "Línea de HEDO fijas de la quincena: se muestra aparte y suma al total de HEDO, nunca a "
            + "los días")
    public record FixedHedoResponse(
            @Schema(description = "Verdadero si el interruptor del rol del trabajador está encendido")
            boolean applies,
            @Schema(description = "Horas fijas; 0 si no aplica", example = "8") int hours) {

        static FixedHedoResponse from(FixedHedoLine line) {
            return new FixedHedoResponse(line.applies(), line.hours());
        }
    }

    @Schema(description = "Ajuste manual: horas que se suman (ADD) o restan (SUBTRACT) a una categoría de la quincena")
    public record AdjustmentResponse(
            @Schema(example = "5") Long id,
            @Schema(description = "Trabajador") UUID workerId,
            @Schema(description = "Quincena a la que pertenece") FortnightResponse fortnight,
            @Schema(description = "Categoría", example = "RNO") OvertimeCategory category,
            @Schema(description = "ADD (más) o SUBTRACT (menos)", example = "ADD") AdjustmentDirection direction,
            @Schema(description = "Horas, mayores que cero", example = "2") int hours,
            @Schema(description = "Motivo", example = "Turno extra no registrado") String reason,
            @Schema(description = "Momento en que se registró", example = "2026-08-16T15:04:05Z") Instant createdAt,
            @Schema(description = "Usuario que lo registró", nullable = true) String createdBy) {

        public static AdjustmentResponse from(OvertimeAdjustment adjustment) {
            return new AdjustmentResponse(adjustment.id(), adjustment.workerId(),
                    FortnightResponse.from(Fortnight.containing(adjustment.fortnightStart())),
                    adjustment.category(), adjustment.direction(), adjustment.hours(), adjustment.reason(),
                    adjustment.createdAt(), adjustment.createdBy());
        }
    }

    @Schema(description = "Día de la quincena cuyas horas faltan, están pendientes o se guardaron sin novedades")
    public record AffectedDateResponse(
            @Schema(example = "2026-08-12") LocalDate date,
            @Schema(description = "MISSING_RECORD (sin registro), PENDING (falló y espera reproceso) o "
                    + "NOVELTIES_NOT_APPLIED (se guardó con la malla, sin reemplazos ni ausentes)",
                    example = "PENDING") IncompleteReason reason) {

        static AffectedDateResponse from(AffectedDate date) {
            return new AffectedDateResponse(date.date(), date.reason());
        }
    }

    @Schema(description = "Sábana quincenal de horas extra de un trabajador, calculada al consultar")
    public record SheetResponse(
            SheetWorkerResponse worker,
            FortnightResponse fortnight,
            @Schema(description = "Todos los días de la quincena en orden de fecha") List<DayResponse> days,
            @Schema(description = "Suma de los días por categoría, sin la línea de HEDO fijas ni los ajustes")
            CategoryHoursResponse subtotals,
            @Schema(description = "Suma de las horas guardadas de los días, sin descontar el descanso", example = "132")
            int grossHours,
            FixedHedoResponse fixedHedo,
            @Schema(description = "Ajustes manuales de la quincena") List<AdjustmentResponse> adjustments,
            @Schema(description = "Ajustes por categoría: lo que suman menos lo que restan")
            CategoryHoursResponse adjustmentsNet,
            @Schema(description = "A pagar por categoría: subtotal + HEDO fijas (en HEDO) + ajustes")
            CategoryHoursResponse totalToPay,
            @Schema(description = "Verdadero si algún día no tiene registro o está pendiente: la sábana puede faltar "
                    + "horas") boolean incomplete,
            @Schema(description = "Verdadero si algún día se guardó sin novedades aplicadas (paso 1 sin confirmar)")
            boolean noveltiesUnconfirmed,
            @Schema(description = "Días afectados con su motivo") List<AffectedDateResponse> affectedDates,
            @Schema(description = "Inicio de cada versión de parámetros usada en la quincena; si hay más de una, "
                    + "cada día usó la suya") List<LocalDate> parameterVersions) {

        public static SheetResponse from(OvertimeSheet sheet) {
            return new SheetResponse(
                    new SheetWorkerResponse(sheet.worker().id(), sheet.worker().fullName(), sheet.worker().roleName()),
                    FortnightResponse.from(sheet.fortnight()),
                    sheet.days().stream().map(DayResponse::from).toList(),
                    CategoryHoursResponse.from(sheet.subtotals()), sheet.grossHours(),
                    FixedHedoResponse.from(sheet.fixedHedo()),
                    sheet.adjustments().stream().map(AdjustmentResponse::from).toList(),
                    CategoryHoursResponse.from(sheet.adjustmentsNet()),
                    CategoryHoursResponse.from(sheet.totalToPay()), sheet.incomplete(), sheet.noveltiesUnconfirmed(),
                    sheet.affectedDates().stream().map(AffectedDateResponse::from).toList(),
                    sheet.parameterVersions());
        }
    }

    @Schema(description = "Resumen de un trabajador en la quincena")
    public record SummaryRowResponse(
            SheetWorkerResponse worker,
            @Schema(description = "Suma de las horas guardadas, sin descontar el descanso", example = "132")
            int grossHours,
            CategoryHoursResponse subtotals,
            FixedHedoResponse fixedHedo,
            CategoryHoursResponse adjustmentsNet,
            @Schema(description = "A pagar por categoría") CategoryHoursResponse totalToPay,
            @Schema(description = "Verdadero si algún día de la quincena no tiene registro o está pendiente")
            boolean incomplete,
            @Schema(description = "Verdadero si algún día se guardó sin novedades aplicadas")
            boolean noveltiesUnconfirmed) {

        static SummaryRowResponse from(OvertimeSheet sheet) {
            return new SummaryRowResponse(
                    new SheetWorkerResponse(sheet.worker().id(), sheet.worker().fullName(), sheet.worker().roleName()),
                    sheet.grossHours(), CategoryHoursResponse.from(sheet.subtotals()),
                    FixedHedoResponse.from(sheet.fixedHedo()), CategoryHoursResponse.from(sheet.adjustmentsNet()),
                    CategoryHoursResponse.from(sheet.totalToPay()), sheet.incomplete(),
                    sheet.noveltiesUnconfirmed());
        }
    }

    @Schema(description = "Resumen de la quincena de todos los trabajadores con horas o ajustes")
    public record SummaryResponse(
            FortnightResponse fortnight,
            @Schema(description = "Un elemento por trabajador, ordenado por nombre") List<SummaryRowResponse> workers,
            @Schema(description = "Verdadero si algún día de la quincena no tiene registro o está pendiente")
            boolean incomplete,
            @Schema(description = "Verdadero si algún día se guardó sin novedades aplicadas")
            boolean noveltiesUnconfirmed,
            @Schema(description = "Días afectados, iguales para todos los trabajadores")
            List<AffectedDateResponse> affectedDates) {

        public static SummaryResponse from(OvertimeSummary summary) {
            return new SummaryResponse(FortnightResponse.from(summary.fortnight()),
                    summary.sheets().stream().map(SummaryRowResponse::from).toList(), summary.incomplete(),
                    summary.noveltiesUnconfirmed(),
                    summary.affectedDates().stream().map(AffectedDateResponse::from).toList());
        }
    }

    @Schema(description = "Versión de los parámetros de la sábana")
    public record ParametersResponse(
            @Schema(example = "1") Long id,
            @Schema(description = "Primer día en que aplica", example = "2025-01-01") LocalDate validFrom,
            @Schema(description = "Hora en que empieza la noche; termina a las 06:00", example = "19")
            int nightStartHour,
            @Schema(description = "Horas de descanso no pagado de un turno largo", example = "1") int restHours,
            @Schema(description = "Horas que debe tener el turno para descontar el descanso", example = "12")
            int restMinShiftHours,
            @Schema(description = "Horas de HEDO fijas de la quincena", example = "8") int fixedHedoHours,
            @Schema(description = "Roles con el interruptor de HEDO fijas encendido; los demás lo tienen apagado")
            List<String> fixedHedoRoles,
            @Schema(example = "2026-08-16T15:04:05Z") Instant createdAt,
            @Schema(nullable = true) String createdBy) {

        public static ParametersResponse from(OvertimeParameters parameters) {
            return new ParametersResponse(parameters.id(), parameters.validFrom(), parameters.nightStartHour(),
                    parameters.restHours(), parameters.restMinShiftHours(), parameters.fixedHedoHours(),
                    parameters.fixedHedoRoles().stream().sorted().toList(), parameters.createdAt(),
                    parameters.createdBy());
        }
    }

    @Schema(description = "Versión de los parámetros dentro del historial, con el último día en que aplica")
    public record ParameterVersionResponse(
            @Schema(example = "1") Long id,
            @Schema(description = "Primer día en que aplica", example = "2025-01-01") LocalDate validFrom,
            @Schema(description = "Último día en que aplica: el día anterior al inicio de la versión siguiente; nulo "
                    + "si no hay una versión posterior", example = "2026-09-09", nullable = true) LocalDate validTo,
            @Schema(description = "Verdadero si es la versión que aplica hoy (hora de Bogotá)", example = "true")
            boolean current,
            @Schema(description = "Hora en que empieza la noche; termina a las 06:00", example = "19")
            int nightStartHour,
            @Schema(description = "Horas de descanso no pagado de un turno largo", example = "1") int restHours,
            @Schema(description = "Horas que debe tener el turno para descontar el descanso", example = "12")
            int restMinShiftHours,
            @Schema(description = "Horas de HEDO fijas de la quincena", example = "8") int fixedHedoHours,
            @Schema(description = "Roles con el interruptor de HEDO fijas encendido; los demás lo tienen apagado",
                    example = "[\"APOYO\"]") List<String> fixedHedoRoles,
            @Schema(description = "Motivo de la versión; nulo si no se dio", example = "Cambio de descanso desde "
                    + "septiembre", nullable = true) String reason,
            @Schema(description = "Usuario que creó la versión", example = "admin", nullable = true) String createdBy,
            @Schema(description = "Momento en que se creó", example = "2026-08-16T15:04:05Z") Instant createdAt) {

        public static ParameterVersionResponse from(OvertimeParameterVersion version) {
            OvertimeParameters parameters = version.parameters();
            return new ParameterVersionResponse(parameters.id(), parameters.validFrom(), version.validTo(),
                    version.current(), parameters.nightStartHour(), parameters.restHours(),
                    parameters.restMinShiftHours(), parameters.fixedHedoHours(),
                    parameters.fixedHedoRoles().stream().sorted().toList(), parameters.reason(),
                    parameters.createdBy(), parameters.createdAt());
        }
    }

    @Schema(description = "Parámetros vigentes en una fecha, los ya programados y el historial")
    public record ParameterOverviewResponse(
            @Schema(description = "Versión vigente en la fecha consultada") ParametersResponse active,
            @Schema(description = "Versiones que empiezan después de la fecha, la más próxima primero")
            List<ParametersResponse> scheduled,
            @Schema(description = "Todas las versiones, de la más reciente a la más antigua")
            List<ParametersResponse> history) {

        public static ParameterOverviewResponse from(OvertimeParameterOverview overview) {
            return new ParameterOverviewResponse(ParametersResponse.from(overview.active()),
                    overview.scheduled().stream().map(ParametersResponse::from).toList(),
                    overview.history().stream().map(ParametersResponse::from).toList());
        }
    }

    @Schema(description = "Nueva versión de los parámetros. Reemplaza a la anterior desde la fecha de vigencia; las "
            + "versiones no se editan")
    public record CreateParametersRequest(
            @Schema(description = "Primer día en que aplica; debe ser posterior a la última fecha con horas "
                    + "registradas", example = "2026-09-10")
            @NotNull(message = "La fecha de vigencia es obligatoria") LocalDate validFrom,
            @Schema(description = "Hora en que empieza la noche, de 12 a 23", example = "19")
            @NotNull(message = "La hora de inicio de la noche es obligatoria") Integer nightStartHour,
            @Schema(description = "Horas de descanso no pagado, de 0 a 4", example = "1")
            @NotNull(message = "Las horas de descanso son obligatorias") Integer restHours,
            @Schema(description = "Horas mínimas del turno para descontar el descanso, de 1 a 24", example = "12")
            @NotNull(message = "El mínimo de horas del turno es obligatorio") Integer restMinShiftHours,
            @Schema(description = "Horas de HEDO fijas de la quincena, de 0 a 80", example = "8")
            @NotNull(message = "Las horas de HEDO fijas son obligatorias") Integer fixedHedoHours,
            @Schema(description = "Roles con el interruptor de HEDO fijas encendido; vacío o ausente: todos apagados",
                    example = "[\"APOYO\"]") List<String> fixedHedoRoles,
            @Schema(description = "Motivo del cambio, de 3 a 255 caracteres; opcional", example = "Cambio de descanso "
                    + "desde septiembre", nullable = true) String reason) {
    }

    @Schema(description = "Ajuste manual de la sábana de un trabajador. Las horas suman (ADD) o restan (SUBTRACT)")
    public record CreateAdjustmentRequest(
            @Schema(description = "Mes de la quincena (yyyy-MM)", example = "2026-08")
            @NotNull(message = "El mes es obligatorio") YearMonth month,
            @Schema(description = "Quincena: 1 (días 1 a 15) o 2 (día 16 al último)", example = "1")
            @NotNull(message = "La quincena es obligatoria") Integer fortnight,
            @Schema(description = "RDF, RNO, RNF, HEDO, HENO, HEDF o HENF", example = "RNO") String category,
            @Schema(description = "ADD (más) o SUBTRACT (menos)", example = "ADD") String direction,
            @Schema(description = "Horas, mayores que cero", example = "2")
            @NotNull(message = "Las horas son obligatorias") Integer hours,
            @Schema(description = "Motivo corto, de 3 a 255 caracteres", example = "Turno extra no registrado")
            String reason) {
    }
}
