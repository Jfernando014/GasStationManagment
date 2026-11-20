package com.edu.unicauca.gasstation.backend.incentives.domain.services;

import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.DIESEL_ID;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.GASOLINE_ID;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.GASOLINE_NAME;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.OCTANE_ID;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.additive;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.clockAt;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.rule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.incentives.BellavistaIncentiveSheet;
import com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.InMemoryCatalog;
import com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.InMemoryRules;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.AdditiveIncentive;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.CreateRuleResult;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.IncentiveRule;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.RuleHistoryEntry;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.RuleStatus;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.RuleWarning;
import com.edu.unicauca.gasstation.backend.incentives.domain.repositories.IncentiveRuleRepository;
import com.edu.unicauca.gasstation.backend.incentives.exception.DuplicateRuleException;
import com.edu.unicauca.gasstation.backend.incentives.exception.IncentiveError;
import com.edu.unicauca.gasstation.backend.incentives.exception.IncentiveException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Creation, listing and history of the incentive rules, and the rule of the last closed journal. */
class IncentiveRuleServiceTest {

    /** April 20th, 10:00 in Bogotá. */
    private static final String NOW = "2026-04-20T15:00:00Z";
    private static final Long INACTIVE_ID = 20L;

    private InMemoryRules rules;
    private InMemoryCatalog catalog;
    /** What the closed journal port answers; the tests change it. */
    private Optional<LocalDate> lastClosed;
    private IncentiveRuleService service;

    @BeforeEach
    void setUp() {
        rules = new InMemoryRules();
        catalog = BellavistaIncentiveSheet.catalog()
                .with("VIEJO", additive(INACTIVE_ID, "Aditivo descontinuado", false, 3_000));
        lastClosed = Optional.empty();
        service = serviceAt(NOW);
    }

    private IncentiveRuleService serviceAt(String instant) {
        return new IncentiveRuleService(rules, catalog, () -> lastClosed, clockAt(instant));
    }

    private IncentiveException failureOf(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        return catchThrowableOfType(call, IncentiveException.class);
    }

    // ---- the validity must be after the last closed journal ----

    @Test
    void aValidityBeforeTheLastClosedJournalIsRefusedAndNothingIsStored() {
        lastClosed = Optional.of(LocalDate.of(2026, 2, 10));

        IncentiveException error = failureOf(() ->
                service.create(GASOLINE_ID, BigDecimal.valueOf(900), LocalDate.of(2026, 2, 1), null));

        assertThat(error.error()).isEqualTo(IncentiveError.VALID_FROM_NOT_AFTER_LAST_CLOSED);
        assertThat(error.getMessage()).contains("2026-02-10");
        assertThat(rules.all()).isEmpty();
    }

    @Test
    void aValidityEqualToTheLastClosedJournalIsRefusedToo() {
        lastClosed = Optional.of(LocalDate.of(2026, 2, 10));

        IncentiveException error = failureOf(() ->
                service.create(GASOLINE_ID, BigDecimal.valueOf(900), LocalDate.of(2026, 2, 10), null));

        assertThat(error.error()).isEqualTo(IncentiveError.VALID_FROM_NOT_AFTER_LAST_CLOSED);
        assertThat(rules.all()).isEmpty();
    }

    @Test
    void aValidityTheDayAfterTheLastClosedJournalIsAccepted() {
        lastClosed = Optional.of(LocalDate.of(2026, 2, 10));

        CreateRuleResult result = service.create(GASOLINE_ID, BigDecimal.valueOf(900), LocalDate.of(2026, 2, 11), null);

        assertThat(result.created()).isTrue();
        assertThat(rules.all()).extracting(IncentiveRule::validFrom).containsExactly(LocalDate.of(2026, 2, 11));
    }

    @Test
    void withoutAnyClosedJournalEveryValidityIsAccepted() {
        lastClosed = Optional.empty();

        CreateRuleResult result = service.create(GASOLINE_ID, BigDecimal.valueOf(900), LocalDate.of(2025, 1, 1), null);

        assertThat(result.created()).isTrue();
    }

    @Test
    void theClosedJournalIsCheckedBeforeLookingForAnExistingRuleOfThatDate() {
        rules.with(rule(GASOLINE_ID, 800, LocalDate.of(2026, 2, 1)));
        lastClosed = Optional.of(LocalDate.of(2026, 2, 10));

        IncentiveException error = failureOf(() ->
                service.create(GASOLINE_ID, BigDecimal.valueOf(900), LocalDate.of(2026, 2, 1), null));

        assertThat(error.error()).isEqualTo(IncentiveError.VALID_FROM_NOT_AFTER_LAST_CLOSED);
    }

    // ---- the additive and the value ----

    @Test
    void aProductThatIsNotAnAdditiveOrDoesNotExistIsNotFound() {
        IncentiveException error = failureOf(() ->
                service.create(777L, BigDecimal.valueOf(900), LocalDate.of(2026, 5, 1), null));

        assertThat(error.error()).isEqualTo(IncentiveError.PRODUCT_NOT_FOUND);
        assertThat(error.getMessage()).contains("777");
        assertThat(rules.all()).isEmpty();
    }

    @Test
    void anInactiveAdditiveCannotGetNewRulesEvenIfTheValueIsWrong() {
        IncentiveException error = failureOf(() ->
                service.create(INACTIVE_ID, BigDecimal.valueOf(-5), LocalDate.of(2026, 5, 1), null));

        assertThat(error.error()).isEqualTo(IncentiveError.PRODUCT_INACTIVE);
        assertThat(error.getMessage()).contains("Aditivo descontinuado");
    }

    @Test
    void anInactiveAdditiveKeepsItsHistory() {
        rules.with(rule(INACTIVE_ID, 500, LocalDate.of(2025, 11, 1)));

        assertThat(service.history(INACTIVE_ID)).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "100001", "800.5", "0.5", "-0.01"})
    void aValueThatIsNotAWholeNumberOfPesosBetweenZeroAndTheLimitIsRefused(String value) {
        IncentiveException error = failureOf(() ->
                service.create(GASOLINE_ID, new BigDecimal(value), LocalDate.of(2026, 5, 1), null));

        assertThat(error.error()).isEqualTo(IncentiveError.INVALID_VALUE_PER_UNIT);
        assertThat(rules.all()).isEmpty();
    }

    @Test
    void aMissingValueIsRefused() {
        IncentiveException error = failureOf(() ->
                service.create(GASOLINE_ID, null, LocalDate.of(2026, 5, 1), null));

        assertThat(error.error()).isEqualTo(IncentiveError.INVALID_VALUE_PER_UNIT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "100000", "800.00", "1000"})
    void zeroTheLimitAndWholeNumbersWrittenWithZeroDecimalsAreAccepted(String value) {
        CreateRuleResult result = service.create(GASOLINE_ID, new BigDecimal(value), LocalDate.of(2026, 5, 1), null);

        assertThat(result.created()).isTrue();
        assertThat(result.rule().valuePerUnit()).isEqualTo(new BigDecimal(value).intValue());
    }

    // ---- duplicates ----

    @Test
    void anAdditiveCannotHaveTwoRulesStartingTheSameDay() {
        rules.with(rule(GASOLINE_ID, 800, LocalDate.of(2026, 5, 1)));

        IncentiveException error = failureOf(() ->
                service.create(GASOLINE_ID, BigDecimal.valueOf(900), LocalDate.of(2026, 5, 1), null));

        assertThat(error.error()).isEqualTo(IncentiveError.RULE_ALREADY_EXISTS);
        assertThat(error.getMessage()).contains(GASOLINE_NAME).contains("2026-05-01");
        assertThat(rules.all()).hasSize(1);
    }

    @Test
    void theSameDayOfAnotherAdditiveIsNotADuplicate() {
        rules.with(rule(GASOLINE_ID, 800, LocalDate.of(2026, 5, 1)));

        CreateRuleResult result = service.create(OCTANE_ID, BigDecimal.valueOf(2_000), LocalDate.of(2026, 5, 1), null);

        assertThat(result.created()).isTrue();
    }

    @Test
    void whenTwoCreationsCollideInTheDatabaseTheLoserGetsTheDuplicateError() {
        IncentiveRuleRepository racing = mock(IncentiveRuleRepository.class);
        when(racing.findHistory(GASOLINE_ID)).thenReturn(List.of());
        when(racing.save(any())).thenThrow(new DuplicateRuleException());
        IncentiveRuleService losing = new IncentiveRuleService(racing, catalog, () -> lastClosed, clockAt(NOW));

        IncentiveException error = failureOf(() ->
                losing.create(GASOLINE_ID, BigDecimal.valueOf(900), LocalDate.of(2026, 5, 1), null));

        assertThat(error.error()).isEqualTo(IncentiveError.RULE_ALREADY_EXISTS);
    }

    @Test
    void aRuleWithTheValueAlreadyInForceIsNotCreatedAndTheAnswerSaysSo() {
        rules.with(rule(GASOLINE_ID, 800, LocalDate.of(2025, 11, 1)));

        CreateRuleResult result = service.create(GASOLINE_ID, BigDecimal.valueOf(800), LocalDate.of(2026, 5, 1), "igual");

        assertThat(result.created()).isFalse();
        assertThat(result.rule()).isNull();
        assertThat(result.warnings()).isEmpty();
        assertThat(result.message()).contains("No se creó").contains(GASOLINE_NAME).contains("2025-11-01");
        assertThat(rules.all()).hasSize(1);
    }

    @Test
    void theSameValueIsOnlyIgnoredWhenItIsTheOneInForceOnThatDateNotTheLatestOne() {
        rules.with(rule(GASOLINE_ID, 800, LocalDate.of(2025, 11, 1)),
                rule(GASOLINE_ID, 1_000, LocalDate.of(2026, 8, 1)));

        // 800 is in force on June 1st: nothing to create, even though a later rule exists
        assertThat(service.create(GASOLINE_ID, BigDecimal.valueOf(800), LocalDate.of(2026, 6, 1), null).created())
                .isFalse();
        // 1,000 is not in force on June 1st: a rule is created
        assertThat(service.create(GASOLINE_ID, BigDecimal.valueOf(1_000), LocalDate.of(2026, 6, 1), null).created())
                .isTrue();
    }

    @Test
    void stoppingTheIncentiveIsARuleOfZeroPesos() {
        rules.with(rule(GASOLINE_ID, 800, LocalDate.of(2025, 11, 1)));

        CreateRuleResult result = service.create(GASOLINE_ID, BigDecimal.ZERO, LocalDate.of(2026, 5, 1), "se acaba");

        assertThat(result.created()).isTrue();
        assertThat(result.rule().valuePerUnit()).isZero();
    }

    // ---- what is stored ----

    @Test
    void theStoredRuleKeepsTheValuesTheTrimmedReasonAndTheMomentOfCreation() {
        CreateRuleResult result = service.create(GASOLINE_ID, BigDecimal.valueOf(900), LocalDate.of(2026, 5, 1),
                "  Sube el precio del aditivo  ");

        assertThat(result.created()).isTrue();
        assertThat(result.message()).isEqualTo("Regla de incentivo creada");
        IncentiveRule stored = result.rule();
        assertThat(stored.id()).isNotNull();
        assertThat(stored.productId()).isEqualTo(GASOLINE_ID);
        assertThat(stored.valuePerUnit()).isEqualTo(900);
        assertThat(stored.validFrom()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(stored.active()).isTrue();
        assertThat(stored.reason()).isEqualTo("Sube el precio del aditivo");
        assertThat(stored.createdAt()).isEqualTo(Instant.parse(NOW));
        assertThat(rules.all()).containsExactly(stored);
    }

    @Test
    void aBlankOrMissingReasonIsStoredAsNull() {
        CreateRuleResult blank = service.create(GASOLINE_ID, BigDecimal.valueOf(900), LocalDate.of(2026, 5, 1), "   ");
        CreateRuleResult missing = service.create(GASOLINE_ID, BigDecimal.valueOf(1_000), LocalDate.of(2026, 6, 1),
                null);

        assertThat(blank.rule().reason()).isNull();
        assertThat(missing.rule().reason()).isNull();
    }

    // ---- warnings that do not block ----

    @Test
    void aValueAboveHalfOfTheSalePriceWarnsButTheRuleIsCreated() {
        // The gasoline additive sells at $4,500: half is $2,250
        CreateRuleResult result = service.create(GASOLINE_ID, BigDecimal.valueOf(2_300), LocalDate.of(2026, 5, 1), null);

        assertThat(result.created()).isTrue();
        assertThat(result.warnings()).extracting(RuleWarning::code)
                .containsExactly(RuleWarning.Code.UNUSUALLY_HIGH_VALUE);
        assertThat(result.warnings().get(0).message()).contains("50 %");
    }

    @Test
    void exactlyHalfOfTheSalePriceDoesNotWarn() {
        CreateRuleResult result = service.create(GASOLINE_ID, BigDecimal.valueOf(2_250), LocalDate.of(2026, 5, 1), null);

        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void aValidityThatIsNotTheFirstDayOfTheMonthWarns() {
        CreateRuleResult result = service.create(GASOLINE_ID, BigDecimal.valueOf(900), LocalDate.of(2026, 5, 15), null);

        assertThat(result.created()).isTrue();
        assertThat(result.warnings()).extracting(RuleWarning::code)
                .containsExactly(RuleWarning.Code.VALID_FROM_NOT_FIRST_DAY);
    }

    @Test
    void bothWarningsCanComeTogether() {
        CreateRuleResult result = service.create(GASOLINE_ID, BigDecimal.valueOf(3_000), LocalDate.of(2026, 5, 15), null);

        assertThat(result.warnings()).extracting(RuleWarning::code)
                .containsExactlyInAnyOrder(RuleWarning.Code.UNUSUALLY_HIGH_VALUE,
                        RuleWarning.Code.VALID_FROM_NOT_FIRST_DAY);
    }

    @Test
    void anAdditiveWithoutPriceNeverWarnsAboutTheValue() {
        catalog.with("SIN-PRECIO", new com.edu.unicauca.gasstation.backend.incentives.domain.ports.AdditiveInfo(
                30L, "Aditivo sin precio", true, null));

        CreateRuleResult result = service.create(30L, BigDecimal.valueOf(100_000), LocalDate.of(2026, 5, 1), null);

        assertThat(result.created()).isTrue();
        assertThat(result.warnings()).isEmpty();
    }

    // ---- list and history ----

    @Test
    void theListShowsEachActiveAdditiveWithItsRuleOnTheDateAndTheNextOne() {
        rules.with(BellavistaIncentiveSheet.rules());

        List<AdditiveIncentive> rows = service.listByDate(LocalDate.of(2026, 2, 15));

        assertThat(rows).extracting(AdditiveIncentive::productName).doesNotContain("Aditivo descontinuado");
        AdditiveIncentive gasoline = rows.stream().filter(row -> row.productId().equals(GASOLINE_ID)).findFirst()
                .orElseThrow();
        assertThat(gasoline.current().valuePerUnit()).isEqualTo(1_000);
        assertThat(gasoline.next().valuePerUnit()).isEqualTo(1_200);
        assertThat(gasoline.hasRules()).isTrue();
        AdditiveIncentive diesel = rows.stream().filter(row -> row.productId().equals(DIESEL_ID)).findFirst()
                .orElseThrow();
        assertThat(diesel.current().valuePerUnit()).isEqualTo(1_500);
        assertThat(diesel.next()).isNull();
    }

    @Test
    void anAdditiveThatNeverHadARuleIsMarkedWithoutIncentive() {
        rules.with(rule(GASOLINE_ID, 800, LocalDate.of(2025, 11, 1)));

        AdditiveIncentive octane = service.listByDate(LocalDate.of(2026, 2, 15)).stream()
                .filter(row -> row.productId().equals(OCTANE_ID)).findFirst().orElseThrow();

        assertThat(octane.hasRules()).isFalse();
        assertThat(octane.current()).isNull();
        assertThat(octane.next()).isNull();
    }

    @Test
    void anAdditiveWhoseFirstRuleStartsLaterHasNoCurrentRuleButHasANextOne() {
        rules.with(rule(GASOLINE_ID, 800, LocalDate.of(2026, 6, 1)));

        AdditiveIncentive gasoline = service.listByDate(LocalDate.of(2026, 2, 15)).stream()
                .filter(row -> row.productId().equals(GASOLINE_ID)).findFirst().orElseThrow();

        assertThat(gasoline.current()).isNull();
        assertThat(gasoline.next().validFrom()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(gasoline.hasRules()).isTrue();
    }

    @Test
    void withoutADateTheListUsesTodayInBogotaNotInUtc() {
        rules.with(rule(GASOLINE_ID, 800, LocalDate.of(2025, 11, 1)),
                rule(GASOLINE_ID, 1_000, LocalDate.of(2026, 4, 30)));

        // 03:00 UTC of May 1st is still April 30th in Bogotá, so the second rule already applies
        AdditiveIncentive lateApril = serviceAt("2026-05-01T03:00:00Z").listByDate(null).stream()
                .filter(row -> row.productId().equals(GASOLINE_ID)).findFirst().orElseThrow();
        // At 10:00 of April 20th it does not
        AdditiveIncentive earlier = service.listByDate(null).stream()
                .filter(row -> row.productId().equals(GASOLINE_ID)).findFirst().orElseThrow();

        assertThat(lateApril.current().valuePerUnit()).isEqualTo(1_000);
        assertThat(earlier.current().valuePerUnit()).isEqualTo(800);
        assertThat(earlier.next().valuePerUnit()).isEqualTo(1_000);
    }

    @Test
    void theHistoryComesNewestFirstWithTheStateOfEachRuleToday() {
        rules.with(rule(GASOLINE_ID, 800, LocalDate.of(2025, 11, 1)),
                rule(GASOLINE_ID, 1_000, LocalDate.of(2026, 2, 1)),
                rule(GASOLINE_ID, 1_200, LocalDate.of(2026, 6, 1)));

        List<RuleHistoryEntry> history = service.history(GASOLINE_ID);

        assertThat(history).extracting(entry -> entry.rule().valuePerUnit(), RuleHistoryEntry::status)
                .containsExactly(
                        tuple(1_200, RuleStatus.PROGRAMADO),
                        tuple(1_000, RuleStatus.VIGENTE),
                        tuple(800, RuleStatus.ANTERIOR));
    }

    @Test
    void theHistoryOfAnUnknownProductIsNotFound() {
        IncentiveException error = failureOf(() -> service.history(777L));

        assertThat(error.error()).isEqualTo(IncentiveError.PRODUCT_NOT_FOUND);
    }
}
