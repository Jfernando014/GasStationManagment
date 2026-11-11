# SCRUM-106: initial backend (HU-15, HU-18, HU-19)

> This document records increment 1. See [increment 2](SCRUM-106-increment-2.md) for the new
> read-only ADMIN endpoint, optional internal parameter integration, main comparison and current verification.

Branch: feature/back/scrum-106-performance. Independent worktree: repo-scrum106.
Base: published origin/feature/back/scrum-107-shift-sales-history, commit
9568be05b6f226b9035f8654724389155e972168. The new branch has no upstream until it is published.
The original repo and repo-scrum107 were preserved. No commit, push, merge or rebase was performed.

## Sources and scope

The original, unchanged workbooks are local references in the original repo/docs/references:

- Historias Usuario Estacion.xlsx: Estadisticas y Ventas (accented sheet name), HU-15 rows 12-13,
  HU-18 rows 19-20, HU-19 row 22.
- 6 OBJETIVOS VENDEDORES BELLAVISTA AGOSTO 2026 (2).xlsx: MODELO NUEVO AL14/AL29 (individual
  compliance), AK14/AK29 (contribution to station target, a different metric), ESCENARIOS B3:B18
  (observed weekday/shift baselines), BASE DE DATOS I2 (observation/baseline), PARAMETROS B71:B72
  (70/30 target distribution, never a night multiplier).

Existing SCRUM-40, SCRUM-83 and SCRUM-107 documentation and code were inspected.
This increment implements internal read use cases and pure calculations in statistics. It does not
add endpoints, frontend, incentives, shared module contracts, a migration or another copy of source sales.
No production workbook values, nightly factors, weekly splits or fabricated attendance are installed.

## History read extension

ShiftSaleHistoryRepository.readRange(fromInclusive, toInclusive) returns ShiftSaleHistoryRange.
Existing writes, locking, recovery, listeners and single-date methods are unchanged.

One PostgreSQL SELECT with a LEFT JOIN reads processing markers and confirmed sales at one MVCC snapshot.
It does not do one query per day or hold a write lock. Every requested operational date is represented:

| Status | Meaning |
| --- | --- |
| PROCESSED | Confirmed journal with individual closes; original snapshots retained exactly. |
| PROCESSED_EMPTY | Confirmed journal with no individual closes. It does not certify individual attendance. |
| PENDING | Processing failed or is outstanding; no unconfirmed sale rows are exposed. |
| ABSENT | No statistics marker; not evidence of a closed empty day, a rest day or zero sales. |

Any pending or absent date makes the requested period incomplete, so no partial-period metrics are
calculated. A wholly confirmed empty range returns NO_DATA without creating seller rows. A recorded
zero close remains a real observation and can have 0% compliance against a positive target.
Inconsistent confirmed counts, dates or duplicate resolved worker/shift identities fail validation.
The inclusive range guard is 1..3660 dates, years 1..9999, following existing statistics limits.

## Application use cases and explicit parameters

SellerPerformanceService uses only statistics' own history port. Its transaction propagation is
NOT_SUPPORTED: no writes or source recovery are performed by a report. A missing/failed database response
propagates as an error instead of becoming an empty successful period. Each use case reads the range once.

Internal methods:

- compliance(Window, Measure, List<IndividualTarget>, displayScale)
- compare(Window, Measure, List<ShiftClassification>, displayScale)
- normalize(Window, Measure, List<ShiftClassification>, List<ShiftMultiplier>, displayScale)

Window is an explicit inclusive period. There is no invented definition of a week, partial week,
daily individual target or global target. A day, week, month or other period can be calculated only
with an individually assigned target for exactly that window and metric.

The parameter records are internal integration values with required provenance, not new public contracts
or an HTTP mechanism for entering sales. There is no production source for these parameters yet.
Passing empty parameter lists leaves the corresponding calculations UNAVAILABLE with a specific reason.

Measure.FUEL_GALLONS sums the four recorded gallon fields; Measure.BASKET_COP uses basketDominusAmountCop.
Tirilla, delivered amounts and basket sales are different observations and are never added together.
A negative component in the selected metric requires an approved adjustment policy and currently makes
that calculation UNAVAILABLE. Other metrics are not changed, and the original signed snapshots remain intact.

### HU-15: same-period individual compliance

An IndividualTarget specifies worker UUID, exact window, metric, nonnegative amount and source.
Sales are grouped by resolved historical UUID, not name or original document. The displayed name is
from the last recorded close of the seller in the requested period.

Compliance = exact recorded sales / assigned individual target * 100.
Missing, mismatched-window, mismatched-unit and zero targets produce a specific unavailable row, never
a substituted target or percentage. No target is divided by 4, 12, calendar days or scheduled shifts.

The ranking sorts available rows by exact cross-product comparison before display rounding.
Unknown rows follow calculated rows; exact ties use UUID for deterministic ordering. Totals retain
BigDecimal precision. Exact numerator/denominator is returned and percentages/means alone use HALF_EVEN
with display scale 0..12, matching the existing project convention.

Report states distinguish AVAILABLE, PARTIAL (some sellers lack calculable parameters), UNAVAILABLE and
NO_DATA. PARTIAL never authorizes calculating a period with missing history. Sellers without recorded
closes are not assigned invented sales or attendance merely because a target was supplied.

### HU-18: equivalent observed shifts, not a reconstructed attendance baseline

ShiftClassification identifies the original close identity, historical DAY/NIGHT period, explicit
REGULAR/HOLIDAY day type and provenance. All closes must have a classification; otherwise comparison
is unavailable rather than silently dropping unknown observations. Conflicting day types for one
operational date or duplicate classification keys are rejected. No current catalog, name-based period
inference, holiday service or presumed historical calendar is substituted.

Cohorts stay separate by weekday, day type, DAY/NIGHT, exact worked code and effective historical role.
Different code durations and titular/support observations are never pooled. This is a conservative
preparation for the final comparison rules, not a claim that every shift code is interchangeable.

The reference baseline is explicitly labeled:
ALL_CONFIRMED_EQUIVALENT_SELLER_SHIFTS_IN_REQUESTED_WINDOW_INCLUDING_SELLER.
It uses only recorded equivalent closes in the same requested window, including zero observations and
the selected seller. Sample counts and exact station/seller totals accompany the means. Means are
weighted by observed shifts, not an unweighted average of seller averages. A single-observation
cohort is visible as such; no additional sample minimum or leave-one-out policy is invented.

Relative index = (seller total * station observation count) /
                 (station total * seller observation count).
The exact ratio is calculated before rounding either mean. A zero station mean leaves the index
unavailable while retaining valid zero totals/averages. These are averages of observed closes, not
averages certified against complete attendance. No Excel support >=10 filter or universal exclusion
of zero observations is silently adopted.

### HU-19: only an explicitly specified multiplication rule

ShiftMultiplier supplies the exact window, metric, DAY/NIGHT period, positive dimensionless multiplier
and provenance. The type explicitly means multiplication; it is not an unspecified factor with a
guessed operation. The provider must supply an approved rule that actually has this meaning.

Normalized value = recorded equivalent-cohort total * explicit multiplier, without rounding.
The report retains raw values, role, worked code, weekday, day type and DAY/NIGHT labeling.
There are no default factors, no inferred day=1/night=anything rule and no use of 70/30 or 50/50.
Missing/mismatched parameters leave normalized values unavailable. DAY/NIGHT results remain separate;
there is no unapproved combined cross-turn ranking or cross-unit normalization.

## Boundaries and pending integration

1. SalesTargetService, SalesDistributionService, SalesTargetDistributionEngine, SalesJourneyStatistics,
   StatisticsSeries and ExactFraction belong to internal sales packages. They cannot be imported into
   statistics. Their policies were inspected; connecting their actual outputs requires a coordinated
   public API from the owning module. No such shared contract was modified here.
2. No automatic source currently supplies assigned targets for a requested worker/window, nor their
   version/provenance. Targets for days/weeks, partial weeks and the meaning of global target need rules.
3. SCRUM-107 stores effective role/code but not historical DAY/NIGHT or holiday classification.
   ShiftCodeInfo.period exists in the public current catalog but does not certify historical values.
   These classifications must be explicitly sourced before real automatic comparisons are available.
4. HU-19's real factors, validity, operation and policy remain undefined. This increment implements only
   mechanics for explicitly approved multipliers, not a completed production normalization policy.
5. The unchanged sales vendor_document VARCHAR(30) still rejects worker UUID strings of 36 characters.
   SCRUM-107 also retains pending incidents for cedulas, unknown/inactive workers and unsupported shifts.
   Thus real producer-to-consumer end-to-end acceptance remains blocked by upstream dependencies.
6. Empty closed journals do not enumerate worked seller shifts. SCRUM-107 cannot by itself fulfill the
   complete attendance/product-detail contracts BasketSalesReadPort and WorkedShiftsReadPort of SCRUM-83.
7. Holiday interpretation, negative adjustments, baseline population/sample policy, target versions,
   week/date alignment (worksheet dates versus operational overnight dates) and a final combined
   normalization/ranking policy need agreement. No approval is inferred from cached workbook values.

HU-15's automatic screen behavior and HU-18/HU-19 user-facing reports are not claimed complete:
there is no parameter integration, endpoint or frontend in this increment.

## Verification

The new tests cover range states/boundaries, exact field round-trips, preserved original snapshots,
corrupt confirmed counts, hidden unconfirmed rows, concurrent commit and rollback; actual PostgreSQL
with the existing Flyway migrations; Spring application service, calculator precision/ranking,
period/unit mismatches, missing inputs, zero denominators, negative observations, explicit day types
and factors, immutable results and source failures.

The first complete selection, before the final holiday-cohort guard, passed 234 tests:
183 existing regression cases and 51 new SCRUM-106 cases, zero failures/errors/skips (1m15s).

~~~powershell
mvn -o '-Dtest=ClosedJournalSnapshotTest,ShiftSale*Test,JournalSalesProcessingTest,SalesStatisticsModularityTest,JournalServiceTest,JournalClosedEventListenerTest,JournalClosingInventoryListenerTest,SalesPublicApiBoundaryTest,SellerPerformance*Test' test
~~~

After adding the holiday guard and two regression cases, the final focused selection is:

~~~powershell
mvn -o '-Dtest=SellerPerformance*Test,ShiftSaleHistoryRangeTest,SalesStatisticsModularityTest' test
~~~

Final focused result: BUILD SUCCESS, 54 tests, 0 failures, 0 errors, 0 skipped. This includes all 53 new
SCRUM-106 cases and the existing Modulith verification. Together with the unchanged 183-case regression,
236 distinct cases have passed across the two selections. Only the new comparison models/calculations and
their fixtures changed after the full run; no SCRUM-107 writer, listener, recovery or migration changed.
No development datasource is loaded by these tests.
The existing PostgreSQL 16 / Flyway 9 support and Java agent warnings do not indicate a test failure.

## Files

Modified (statistics only):
- domain/repositories/ShiftSaleHistoryRepository.java
- infrastructure/persistence/adapters/ShiftSaleHistoryRepositoryAdapter.java

Added production (statistics):
- domain/models/ShiftSaleHistoryRange.java
- domain/calculation/SellerPerformance.java
- domain/calculation/SellerPerformanceCalculator.java
- application/SellerPerformanceService.java

Added tests (statistics):
- SellerPerformanceTestData.java
- domain/models/ShiftSaleHistoryRangeTest.java
- domain/calculation/SellerPerformanceCalculatorTest.java
- application/SellerPerformanceServiceTest.java
- infrastructure/persistence/SellerPerformancePersistenceTest.java

Added documentation: docs/SCRUM-106-implementation.md.
