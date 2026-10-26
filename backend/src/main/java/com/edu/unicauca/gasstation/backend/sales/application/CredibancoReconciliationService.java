package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterKey;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.*;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.CardReconciliationRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.QrPointsRepository;
import com.edu.unicauca.gasstation.backend.shifts.catalog.ShiftExternalService;
import com.edu.unicauca.gasstation.backend.shifts.spi.ScheduleWorkerDirectory;
import com.edu.unicauca.gasstation.backend.shifts.spi.ScheduleWorkerInfo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HU-34: vouchers by franchise per vendor against the planilla, day result, month totals and commission. */
@Service
@RequiredArgsConstructor
public class CredibancoReconciliationService {
    private final CardReconciliationRepository cards;
    private final QrPointsRepository qrPoints;
    private final SalesParameterService parameters;
    private final ScheduleWorkerDirectory workers;
    private final ShiftExternalService shifts;

    /** Saves (or replaces) one vendor's figures of the day and returns the recalculated day. */
    @Transactional
    public CredibancoDayResult saveVendor(LocalDate date, VendorDeclaration declaration) {
        ScheduleWorkerInfo worker = workers.getWorkersByIds(List.of(declaration.workerId())).get(declaration.workerId());
        if (worker == null) {
            throw new ReconciliationValidationException("workerId", "El trabajador no existe");
        }
        if (!worker.active()) {
            throw new ReconciliationValidationException("workerId", "El trabajador está inactivo");
        }
        if (declaration.shiftCode() != null && shifts.findShiftCodeIdByCode(declaration.shiftCode()).isEmpty()) {
            throw new ReconciliationValidationException("shiftCode",
                    "El código de turno '" + declaration.shiftCode() + "' no existe en el catálogo de turnos");
        }
        cards.upsert(date, declaration);
        return get(date);
    }

    @Transactional
    public CredibancoDayResult removeVendor(LocalDate date, UUID workerId) {
        if (!cards.delete(date, workerId)) {
            throw new ReconciliationValidationException("workerId", "Ese vendedor no tiene datos el " + date);
        }
        return get(date);
    }

    /** Opening figures of a month, used only while that month has no daily rows in the system. */
    @Transactional
    public void saveBaseline(YearMonth month, PeriodTotals totals) {
        cards.saveBaseline(month, totals);
    }

    @Transactional(readOnly = true)
    public CredibancoDayResult get(LocalDate date) {
        List<VendorDeclaration> declarations = cards.findByDate(date);
        Map<UUID, String> names = names(declarations);

        BigDecimal planillaQr = declarations.stream().map(VendorDeclaration::planillaQr)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // QR of the day: Redeban when its file was imported, otherwise what the planillas declare
        BigDecimal redebanQr = qrPoints.findRedeban(date).stream().map(t -> t.transaction().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal dayQr = redebanQr.signum() > 0 ? redebanQr : planillaQr;

        DayFranchiseTotals previousDay = cards.findDailyTotals(date.minusDays(1), date.minusDays(1))
                .getOrDefault(date.minusDays(1), DayFranchiseTotals.ZERO);
        YearMonth month = YearMonth.from(date);
        Map<LocalDate, DayFranchiseTotals> monthBefore = cards.findDailyTotals(month.atDay(1), date.minusDays(1));
        BigDecimal rate = parameters.effective(SalesParameterKey.CREDIBANCO_COMMISSION_RATE, date).value();

        return CredibancoReconciliationCalculator.calculate(date, declarations, names, dayQr, previousDay,
                monthBefore, previousMonth(month.minusMonths(1)), rate);
    }

    private PeriodTotals previousMonth(YearMonth month) {
        Map<LocalDate, DayFranchiseTotals> days = cards.findDailyTotals(month.atDay(1), month.atEndOfMonth());
        if (!days.isEmpty()) {
            DayFranchiseTotals sum = days.values().stream().reduce(DayFranchiseTotals.ZERO, DayFranchiseTotals::plus);
            return PeriodTotals.of(sum.mastercard(), sum.visa(), sum.amex(), sum.qr(), "DAILY_DATA");
        }
        return cards.findPreviousMonthBaseline(month).orElse(PeriodTotals.NONE);
    }

    private Map<UUID, String> names(List<VendorDeclaration> declarations) {
        var ids = declarations.stream().map(VendorDeclaration::workerId).distinct().toList();
        Map<UUID, String> names = new HashMap<>();
        workers.getWorkersByIds(ids).forEach((id, info) -> names.put(id, info.fullName()));
        return names;
    }
}
