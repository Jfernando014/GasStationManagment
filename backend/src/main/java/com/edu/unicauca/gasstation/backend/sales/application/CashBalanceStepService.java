package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.sales.CashBalanceQuery;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo;
import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalSnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.models.CashDifferenceStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.models.DominusCashBalance;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DominusCashBalanceRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.JournalDateReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.services.CashBalanceCalculator;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalStepService;
import com.edu.unicauca.gasstation.backend.sales.exception.CashBalanceJournalNotFoundException;
import com.edu.unicauca.gasstation.backend.sales.exception.CashBalanceNotEditableException;
import com.edu.unicauca.gasstation.backend.sales.exception.CashBalanceNotReadyException;
import com.edu.unicauca.gasstation.backend.sales.exception.JournalNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Step 4 of the journal: cash balance (C-06).
 *
 * <p>Shows the table (station cash, planilla balance, accumulated savings, expenses of the day and the difference
 * against Dominus), stores the Dominus balance and completes the step. A difference, even a non-zero one, does not
 * block the step: what the step needs is that the difference could be calculated. Steps 2 and 3 must already be
 * completed, which {@link JournalStepService} checks.
 */
@Service
public class CashBalanceStepService {

    static final int STEP_NUMBER = 4;

    private final CashBalanceQuery cashBalanceQuery;
    private final JournalDateReadPort journals;
    private final JournalRepository journalRepository;
    private final DominusCashBalanceRepository dominus;
    private final JournalStepService journalStepService;

    public CashBalanceStepService(CashBalanceQuery cashBalanceQuery, JournalDateReadPort journals,
                                  JournalRepository journalRepository, DominusCashBalanceRepository dominus,
                                  JournalStepService journalStepService) {
        this.cashBalanceQuery = cashBalanceQuery;
        this.journals = journals;
        this.journalRepository = journalRepository;
        this.dominus = dominus;
        this.journalStepService = journalStepService;
    }

    /**
     * The table of the day and whether the step can be completed.
     *
     * @param table            table 4c
     * @param canComplete      true when the difference against Dominus could be calculated
     * @param missing          what is still missing, one sentence each; empty when {@code canComplete}
     * @param differenceStatus CUADRA, PLANILLA_MAYOR or PLANILLA_MENOR; null while the difference is unavailable
     */
    public record Evaluation(CashBalanceTableInfo table, boolean canComplete, List<String> missing,
                             CashDifferenceStatus differenceStatus) {
    }

    @Transactional(readOnly = true)
    public Evaluation evaluate(LocalDate date) {
        return evaluate(requireTable(date));
    }

    /** Stores (or replaces) the Dominus accumulated balance of the day and returns the table recalculated. */
    @Transactional
    public Evaluation saveDominusBalance(LocalDate date, BigDecimal accumulatedBalance) {
        JournalSnapshot journal = journals.findSnapshotByDate(date)
                .orElseThrow(() -> new CashBalanceJournalNotFoundException(date));
        if (journal.status() == JournalStatus.CLOSED) {
            throw new CashBalanceNotEditableException("La jornada del " + date + " está cerrada: no se puede "
                    + "cambiar el saldo de Dominus.");
        }
        boolean stepCompleted = journal.steps().stream()
                .anyMatch(step -> STEP_NUMBER == step.stepNumber() && step.status() == StepStatus.COMPLETED);
        if (stepCompleted) {
            throw new CashBalanceNotEditableException("El paso 4 ya está completado: no se puede cambiar el "
                    + "saldo de Dominus.");
        }
        dominus.save(new DominusCashBalance(date, accumulatedBalance));
        return evaluate(requireTable(date));
    }

    /** Completes step 4 when the difference against Dominus can be calculated. */
    @Transactional
    public JournalStep complete(Long journalId) {
        Journal journal = journalRepository.findById(journalId)
                .orElseThrow(() -> new JournalNotFoundException(journalId));
        Evaluation evaluation = evaluate(requireTable(journal.getDate()));
        if (!evaluation.canComplete()) {
            throw new CashBalanceNotReadyException(evaluation.missing());
        }
        return journalStepService.markStepAsCompleted(journalId, STEP_NUMBER);
    }

    private CashBalanceTableInfo requireTable(LocalDate date) {
        return cashBalanceQuery.findDailyCashBalance(date)
                .orElseThrow(() -> new CashBalanceJournalNotFoundException(date));
    }

    private static Evaluation evaluate(CashBalanceTableInfo table) {
        List<String> missing = new ArrayList<>();
        if (table.dailyCash() == null) {
            missing.add("Faltan los cierres de los vendedores del día (paso 2): sin ellos no hay efectivo de la "
                    + "estación.");
        } else if (table.planillaCashBalance() == null) {
            missing.add("Falta registrar el saldo inicial de caja (movimiento «Saldo inicial») con la fecha del "
                    + "primer día de operación, o la fecha es anterior a ese día.");
        }
        if (table.dominusAccumulatedBalance() == null) {
            missing.add("Falta digitar el saldo acumulado de Dominus.");
        }
        CashDifferenceStatus status = table.difference() == null ? null
                : CashBalanceCalculator.statusOf(table.difference());
        return new Evaluation(table, table.difference() != null, List.copyOf(missing), status);
    }
}
