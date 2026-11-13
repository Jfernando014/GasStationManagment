package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterKey;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.MonthlyReconciliation;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.MonthlyReconciliationCalculator;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.CardReconciliationRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.QrPointsRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * C-05: monthly table of Credibanco, QR and Puntos Colombia with the commission. Read only: it sums the daily
 * reconciliations (HU-34 and HU-35), so it needs no data of its own.
 */
@Service
@RequiredArgsConstructor
public class MonthlyReconciliationService {

    private final CardReconciliationRepository cards;
    private final QrPointsRepository qrPoints;
    private final SalesParameterService parameters;

    /** Uses the commission rate effective on the last day of the month. */
    @Transactional(readOnly = true)
    public MonthlyReconciliation get(YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        BigDecimal rate = parameters.effective(SalesParameterKey.CREDIBANCO_COMMISSION_RATE, to).value();
        return MonthlyReconciliationCalculator.calculate(month, cards.findDailyTotals(from, to),
                qrPoints.findRedebanTotals(from, to), qrPoints.findPointsTotals(from, to), rate);
    }
}
