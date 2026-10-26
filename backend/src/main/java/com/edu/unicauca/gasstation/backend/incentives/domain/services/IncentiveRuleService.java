package com.edu.unicauca.gasstation.backend.incentives.domain.services;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.AdditiveIncentive;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.CreateRuleResult;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.IncentiveRule;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.RuleHistoryEntry;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.RuleTimeline;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.RuleWarning;
import com.edu.unicauca.gasstation.backend.incentives.domain.ports.AdditiveCatalogPort;
import com.edu.unicauca.gasstation.backend.incentives.domain.ports.AdditiveInfo;
import com.edu.unicauca.gasstation.backend.incentives.domain.ports.ClosedJournalPort;
import com.edu.unicauca.gasstation.backend.incentives.domain.repositories.IncentiveRuleRepository;
import com.edu.unicauca.gasstation.backend.incentives.exception.DuplicateRuleException;
import com.edu.unicauca.gasstation.backend.incentives.exception.IncentiveError;
import com.edu.unicauca.gasstation.backend.incentives.exception.IncentiveException;
import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lists additives with their incentive, shows a rule history and creates rules. "Today" is Bogotá time. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncentiveRuleService {

    private static final BigDecimal HIGH_VALUE_SHARE_OF_PRICE = new BigDecimal("0.5");

    private final IncentiveRuleRepository repository;
    private final AdditiveCatalogPort catalog;
    private final ClosedJournalPort closedJournal;
    private final Clock clock;

    /** One row per active additive, with its rule on {@code date} (today if null) and the next scheduled one. */
    public List<AdditiveIncentive> listByDate(LocalDate date) {
        LocalDate day = date != null ? date : today();
        List<AdditiveInfo> additives = catalog.listActiveAdditives();
        Map<Long, List<IncentiveRule>> rulesByProduct = repository
                .findByProductIds(additives.stream().map(AdditiveInfo::id).toList()).stream()
                .collect(Collectors.groupingBy(IncentiveRule::productId));
        return additives.stream()
                .map(additive -> toRow(additive, RuleTimeline.of(rulesByProduct.getOrDefault(additive.id(), List.of())), day))
                .toList();
    }

    /** Rules of one additive, newest first, with their state today. Works for inactive additives too. */
    public List<RuleHistoryEntry> history(Long productId) {
        requireAdditive(productId);
        return RuleTimeline.of(repository.findHistory(productId)).historyOn(today());
    }

    /** Creates a rule, or answers {@code created=false} if the additive already earns that value on that date. */
    @Transactional
    public CreateRuleResult create(Long productId, BigDecimal valuePerUnit, LocalDate validFrom, String reason) {
        AdditiveInfo additive = requireAdditive(productId);
        if (!additive.active()) {
            throw new IncentiveException(IncentiveError.PRODUCT_INACTIVE, additive.name());
        }
        int value = requireValidValue(valuePerUnit);
        closedJournal.lastClosedDate()
                .filter(lastClosed -> !validFrom.isAfter(lastClosed))
                .ifPresent(lastClosed -> {
                    throw new IncentiveException(IncentiveError.VALID_FROM_NOT_AFTER_LAST_CLOSED, lastClosed);
                });
        RuleTimeline timeline = RuleTimeline.of(repository.findHistory(productId));
        if (timeline.startingOn(validFrom).isPresent()) {
            throw new IncentiveException(IncentiveError.RULE_ALREADY_EXISTS, additive.name(), validFrom);
        }
        Optional<IncentiveRule> applying = timeline.effectiveOn(validFrom);
        if (applying.isPresent() && applying.get().valuePerUnit() == value) {
            return new CreateRuleResult(false, null,
                    "No se creó la regla: desde el %s el aditivo '%s' ya paga %s por unidad (vigente desde el %s)"
                            .formatted(validFrom, additive.name(), pesos(value), applying.get().validFrom()),
                    List.of());
        }
        IncentiveRule stored;
        try {
            stored = repository.save(
                    IncentiveRule.create(productId, value, validFrom, normalize(reason), Instant.now(clock)));
        } catch (DuplicateRuleException e) {
            throw new IncentiveException(IncentiveError.RULE_ALREADY_EXISTS, additive.name(), validFrom);
        }
        return new CreateRuleResult(true, stored, "Regla de incentivo creada", warnings(additive, value, validFrom));
    }

    private AdditiveIncentive toRow(AdditiveInfo additive, RuleTimeline timeline, LocalDate day) {
        return new AdditiveIncentive(additive.id(), additive.name(), timeline.effectiveOn(day).orElse(null),
                timeline.nextAfter(day).orElse(null), !timeline.isEmpty());
    }

    private AdditiveInfo requireAdditive(Long productId) {
        AdditiveInfo product = catalog.findById(productId)
                .orElseThrow(() -> new IncentiveException(IncentiveError.PRODUCT_NOT_FOUND, productId));
        if (product.category() != ProductCategory.ADITIVO) {
            throw new IncentiveException(IncentiveError.PRODUCT_NOT_ADDITIVE, product.name());
        }
        return product;
    }

    private static int requireValidValue(BigDecimal valuePerUnit) {
        if (valuePerUnit == null || valuePerUnit.stripTrailingZeros().scale() > 0
                || valuePerUnit.signum() < 0
                || valuePerUnit.compareTo(BigDecimal.valueOf(IncentiveRule.MAX_VALUE_PER_UNIT)) > 0) {
            throw new IncentiveException(IncentiveError.INVALID_VALUE_PER_UNIT, number(IncentiveRule.MAX_VALUE_PER_UNIT));
        }
        return valuePerUnit.intValueExact();
    }

    /** Likely slips that are reported but do not block the rule. */
    private static List<RuleWarning> warnings(AdditiveInfo additive, int value, LocalDate validFrom) {
        List<RuleWarning> warnings = new ArrayList<>();
        if (additive.price() != null
                && BigDecimal.valueOf(value).compareTo(additive.price().multiply(HIGH_VALUE_SHARE_OF_PRICE)) > 0) {
            warnings.add(new RuleWarning(RuleWarning.Code.UNUSUALLY_HIGH_VALUE,
                    "Valor inusualmente alto: %s por unidad supera el 50 %% del precio de venta del aditivo (%s)"
                            .formatted(pesos(value), pesos(additive.price().longValue()))));
        }
        if (validFrom.getDayOfMonth() != 1) {
            warnings.add(new RuleWarning(RuleWarning.Code.VALID_FROM_NOT_FIRST_DAY,
                    "La vigencia no es el día 1 del mes; el cliente acostumbra cambiar los valores por mes"));
        }
        return warnings;
    }

    private static String pesos(long amount) {
        return "$" + number(amount);
    }

    private static String number(long amount) {
        return String.format(Locale.forLanguageTag("es-CO"), "%,d", amount);
    }

    private static String normalize(String reason) {
        return reason == null || reason.isBlank() ? null : reason.strip();
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
