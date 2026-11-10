package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.sales.JournalClosedEvent;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterKey;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovement;
import com.edu.unicauca.gasstation.backend.sales.domain.services.SavingsFundService;
import java.math.BigDecimal;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Registers the daily contribution to the savings fund when a journal is closed.
 *
 * <p>The amount is the {@code DAILY_SAVINGS} parameter in force on the date of the journal, so changing the parameter
 * later does not alter contributions already recorded. Repeated events and contributions entered manually for the
 * same day do not create a second contribution.
 */
@Component
public class SavingsContributionOnJournalClosed {

    static final String DESCRIPTION = "Aporte diario al cierre de la jornada";

    private static final Logger LOG = LoggerFactory.getLogger(SavingsContributionOnJournalClosed.class);

    private final SalesParameterService parameters;
    private final SavingsFundService savingsFund;

    public SavingsContributionOnJournalClosed(SalesParameterService parameters, SavingsFundService savingsFund) {
        this.parameters = parameters;
        this.savingsFund = savingsFund;
    }

    @ApplicationModuleListener
    public void on(JournalClosedEvent event) {
        BigDecimal amount = parameters.effective(SalesParameterKey.DAILY_SAVINGS, event.date()).value();
        Optional<SavingsFundMovement> registered =
                savingsFund.registerContributionIfAbsent(event.date(), amount, DESCRIPTION);
        if (registered.isPresent()) {
            LOG.info("Journal {} closed: contribution of {} registered to the savings fund for {}",
                    event.journalId(), amount, event.date());
        } else {
            LOG.info("Journal {} closed: the savings fund already has a contribution for {}, nothing to add",
                    event.journalId(), event.date());
        }
    }
}
