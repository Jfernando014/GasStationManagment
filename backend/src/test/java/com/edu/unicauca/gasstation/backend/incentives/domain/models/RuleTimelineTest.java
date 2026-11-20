package com.edu.unicauca.gasstation.backend.incentives.domain.models;

import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.GASOLINE_ID;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.rule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleTimelineTest {

    private static final LocalDate NOV_1 = LocalDate.of(2025, 11, 1);
    private static final LocalDate FEB_1 = LocalDate.of(2026, 2, 1);
    private static final LocalDate MAR_1 = LocalDate.of(2026, 3, 1);

    private final IncentiveRule november = rule(GASOLINE_ID, 800, NOV_1);
    private final IncentiveRule february = rule(GASOLINE_ID, 1_000, FEB_1);
    private final IncentiveRule march = rule(GASOLINE_ID, 1_200, MAR_1);

    @Test
    void anEmptyTimelineAppliesNothing() {
        RuleTimeline timeline = RuleTimeline.of(List.of());

        assertThat(timeline.isEmpty()).isTrue();
        assertThat(timeline.effectiveOn(FEB_1)).isEmpty();
        assertThat(timeline.nextAfter(FEB_1)).isEmpty();
        assertThat(timeline.startingOn(FEB_1)).isEmpty();
        assertThat(timeline.historyOn(FEB_1)).isEmpty();
    }

    @Test
    void theRuleInForceIsTheLatestOneThatStartedOnOrBeforeTheDate() {
        RuleTimeline timeline = RuleTimeline.of(List.of(november, february, march));

        assertThat(timeline.effectiveOn(LocalDate.of(2025, 10, 31))).isEmpty();
        assertThat(timeline.effectiveOn(NOV_1)).contains(november);
        assertThat(timeline.effectiveOn(LocalDate.of(2026, 1, 31))).contains(november);
        assertThat(timeline.effectiveOn(FEB_1)).contains(february);
        assertThat(timeline.effectiveOn(LocalDate.of(2026, 2, 28))).contains(february);
        assertThat(timeline.effectiveOn(MAR_1)).contains(march);
        assertThat(timeline.effectiveOn(LocalDate.of(2030, 1, 1))).contains(march);
    }

    @Test
    void theRulesAreOrderedByValidityWhateverTheOrderTheyComeIn() {
        RuleTimeline timeline = RuleTimeline.of(List.of(march, november, february));

        assertThat(timeline.effectiveOn(LocalDate.of(2026, 2, 15))).contains(february);
        assertThat(timeline.historyOn(MAR_1)).extracting(entry -> entry.rule().valuePerUnit())
                .containsExactly(1_200, 1_000, 800);
    }

    @Test
    void theNextRuleIsTheNearestOneThatStartsStrictlyAfterTheDate() {
        RuleTimeline timeline = RuleTimeline.of(List.of(november, february, march));

        assertThat(timeline.nextAfter(LocalDate.of(2025, 10, 1))).contains(november);
        assertThat(timeline.nextAfter(NOV_1)).contains(february);
        assertThat(timeline.nextAfter(LocalDate.of(2026, 1, 31))).contains(february);
        assertThat(timeline.nextAfter(FEB_1)).contains(march);
        assertThat(timeline.nextAfter(MAR_1)).isEmpty();
    }

    @Test
    void aRuleStartingExactlyOnADateIsFoundOnlyOnThatDate() {
        RuleTimeline timeline = RuleTimeline.of(List.of(november, february));

        assertThat(timeline.startingOn(FEB_1)).contains(february);
        assertThat(timeline.startingOn(LocalDate.of(2026, 2, 2))).isEmpty();
        assertThat(timeline.startingOn(LocalDate.of(2026, 1, 31))).isEmpty();
    }

    @Test
    void theHistoryIsNewestFirstWithTheStateOfEachRuleToday() {
        RuleTimeline timeline = RuleTimeline.of(List.of(november, february, march));

        List<RuleHistoryEntry> history = timeline.historyOn(LocalDate.of(2026, 2, 15));

        assertThat(history).extracting(entry -> entry.rule().validFrom(), RuleHistoryEntry::status)
                .containsExactly(
                        tuple(MAR_1, RuleStatus.PROGRAMADO),
                        tuple(FEB_1, RuleStatus.VIGENTE),
                        tuple(NOV_1, RuleStatus.ANTERIOR));
    }

    @Test
    void aRuleThatStartsTodayIsTheOneInForce() {
        RuleTimeline timeline = RuleTimeline.of(List.of(november, february));

        assertThat(timeline.historyOn(FEB_1)).extracting(RuleHistoryEntry::status)
                .containsExactly(RuleStatus.VIGENTE, RuleStatus.ANTERIOR);
    }

    @Test
    void whenEveryRuleStartsInTheFutureNoneIsInForce() {
        RuleTimeline timeline = RuleTimeline.of(List.of(february, march));

        assertThat(timeline.historyOn(NOV_1)).extracting(RuleHistoryEntry::status)
                .containsExactly(RuleStatus.PROGRAMADO, RuleStatus.PROGRAMADO);
    }
}
