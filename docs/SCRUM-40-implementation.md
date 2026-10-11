# SCRUM-40 implementation log

## Scope and status

Baseline inspected: commit `232688e` on `feature/back/nicolas_dev`.

- HU-14: increments 1 and 2 provide pure domain types; increment 3A adds the PostgreSQL schema, a separate decimal storage policy, and migration/constraint tests. Registration and JPA mapping remain pending.
- HU-16: pending; requires real historical sales from the owning sales module.
- HU-17: pending; requires approved scenario rules and the same real sales source.

Increment 3A baseline: clean Git checkout at `49aae77` on `feature/back/sales-targets-statistics`; V9 was available.
V9 now defines `sales_targets`; it was executed only in a disposable test container.
No JPA entities, repositories, endpoints, registration services, individual calculations or other-module changes
are introduced. The development database and credentials were not used or modified. V1 through V8 are unchanged.

## Approved decisions

- Configured targets are distinct from calculated individual targets.
- Annual and monthly targets are independent. Annual targets are not automatically divided into twelve months.
- Valid calendar months are 1 through 12. An absent month represents an annual period.
- Aggregate categories are fuel and basket. Individual fuel products are MOTOR, DIESEL, MAXPRO and EXTRA.
- Hourly weights, shifts and titular/support distribution belong to a later increment.
- Business analysis and Excel formulas are coordinated outside Codex.
- Later historical queries must use the actual sales source rather than introduce a parallel source.
- Cross-module shift access must use public contracts, not internal repositories.
- Aggregate fuel targets use gallons; basket targets use Colombian pesos (COP).
- Configured values use BigDecimal and are preserved without rounding or conversion.
- Identity, value and unit are required. Negative values are rejected. Zero is accepted provisionally;
  no positive-only business rule has been approved.
- August 2026 examples (26920 gallons and 8400000 COP) are test data, not system constants or defaults.
- Provisional storage is NUMERIC(30,12), with exact numeric representability and absolute value strictly below 10^18.
- More than twelve decimal places are accepted only when removing the extra places loses no numeric value.
- Storage validation is separate from SalesTarget. It never modifies or normalizes the original BigDecimal.
- PostgreSQL retrieval requires numeric equality, not preservation of the original decimal scale.
- Annual uniqueness is year/category; monthly uniqueness is year/month/category. Both use partial unique indexes.
- The database generates the identity and defaults created_at to CURRENT_TIMESTAMP (TIMESTAMPTZ).

## Domain model

All production files below are under
`backend/src/main/java/com/edu/unicauca/gasstation/backend/sales/domain/models/`.

| File | Responsibility |
| --- | --- |
| `SalesTargetPeriod.java` | Immutable year and optional month, annual/monthly factories and calendar-month validation. No business year limits. |
| `SalesTargetCategory.java` | Aggregate categories `FUEL_AGGREGATE` and `BASKET`. |
| `FuelProduct.java` | Separate Excel product vocabulary. Does not introduce product-specific targets. |
| `SalesTargetIdentity.java` | Immutable conceptual period/category identity; requires both components. Not a final database key. |
| `SalesTargetUnit.java` | Units `GALLONS` and `COP`. Added in increment 2. |
| `SalesTarget.java` | Immutable record containing identity, BigDecimal value and unit; validates non-null components, nonnegative values and category/unit combinations. Added in increment 2. |

The classes use only Java types, with no Spring, JPA, Lombok or other-module imports.
The period representation uses a nullable `Integer` month; this is an in-memory choice, not a database design.
Record equality distinguishes categories, years and months. Required identity components are structural requirements,
not editing policies. Increment 2 validates amounts in `SalesTarget`, without modifying the increment 1 types.

`MAXPRO` preserves the Excel terminology. Inventory currently names its enum value `MAX_PRO`.
No conversion or dependency on inventory is introduced in this increment.

## Pending decisions

- Meaning, units and application of maximum sales.
- Whether zero remains valid as a final business rule and any future business amount limits or rounding policy;
  the provisional technical storage limits are approved for increment 3A.
- Whether configured fuel targets will be broken down by individual product.
- JPA mapping, registration/query contracts, additional audit requirements and future schema evolution.
- Editing lock rules, permitted operations and authorized users.
- Monthly consistency rules, if any, relative to an annual target.
- Hourly weights, parameter versions, titular/support split applicability and individual results.
- Manual adjustments, holidays, operational date of night shifts and historical-average denominator.
- Public assignment query contract in shifts and public historical-sales contract in sales.

No default percentages, formulas, lock rules, maximum-sales field or individual calculations are implemented.
Units are explicit, never inferred or converted. The only amount restriction is rejection of negative values.
Valid combinations are FUEL_AGGREGATE/GALLONS and BASKET/COP.

## Tests

Test files under `backend/src/test/java/com/edu/unicauca/gasstation/backend/sales/domain/models/`:

- `SalesTargetPeriodTest.java`: annual/monthly distinction, all twelve months, invalid months and constructor validation.
- `SalesTargetIdentityTest.java`: identity distinctions, required components and separation of categories/products.
- `SalesTargetTest.java`: valid annual/monthly targets, Excel examples, negatives, zero, nulls, incorrect units and exact decimal preservation.

Increment 1 execution, from `backend` with Java 21 and Maven 3.9.11:

```powershell
mvn -o '-Dtest=SalesTargetPeriodTest,SalesTargetIdentityTest' test
```

Result: **BUILD SUCCESS**, 22 tests, 0 failures, 0 errors, 0 skipped.
`SalesTargetPeriodTest`: 19 cases; `SalesTargetIdentityTest`: 3 cases.
Maven compiled the main and test sources, then ran only the selected pure domain tests.
No Spring context, database, Flyway migration or other test suite was executed.
Offline mode used existing cached dependencies; no dependencies were installed or downloaded.
Build output was generated under the existing `backend/target` directory.
The initial unquoted test-selector command was rejected by PowerShell before Maven ran; quoting the argument
resolved that shell parsing issue.

Increment 2 execution, from `backend`, using only existing cached dependencies:

```powershell
mvn -o '-Dtest=SalesTargetPeriodTest,SalesTargetIdentityTest,SalesTargetTest' test
```

Result: **BUILD SUCCESS**, 37 tests, 0 failures, 0 errors, 0 skipped.
`SalesTargetPeriodTest`: 19 cases; `SalesTargetIdentityTest`: 3 cases; `SalesTargetTest`: 15 cases.
Only these three SCRUM-40 classes ran. No Spring context, database or migration was executed.
The compiler emitted a warning about implicitly compiled files not being subject to annotation processing;
compilation and the selected tests still succeeded. No dependency or compiler configuration was changed.

## Change log

- 2026-10-10: created this document, four pure Java domain types and two JUnit test classes within the authorized scope.
  No persistence or business calculation was introduced.
- 2026-10-10: executed the selected tests offline successfully and recorded the real results above.
- Increment 2: inspected the four existing domain types; no incompatibility found and no changes to them required.
  Added `SalesTargetUnit.java` and `SalesTarget.java` under sales/domain/models and `SalesTargetTest.java`
  under the corresponding test package. Updated only this document in addition to those new files.
  Offline execution of the three SCRUM-40 test classes succeeded: 37 cases, including 15 new cases,
  with no failures, errors or skipped tests.

## Increment 3A: schema and storage policy

Files created:

- `backend/src/main/resources/db/migration/V9__create_sales_targets.sql`
- `backend/src/main/java/com/edu/unicauca/gasstation/backend/sales/domain/services/SalesTargetStoragePolicy.java`
- `backend/src/test/java/com/edu/unicauca/gasstation/backend/sales/domain/services/SalesTargetStoragePolicyTest.java`
- `backend/src/test/java/com/edu/unicauca/gasstation/backend/sales/infrastructure/persistence/SalesTargetMigrationTest.java`

Only this document was modified. Existing domain records and migration files were not changed.

SQL constraints enforce an optional month in 1..12, nonnegative values excluding NaN, and exactly the
FUEL_AGGREGATE/GALLONS and BASKET/COP combinations. `uq_sales_targets_annual` and
`uq_sales_targets_monthly` enforce uniqueness independently for annual and monthly rows.
No update/delete functionality or database permission policy is introduced.

SalesTargetStoragePolicy validates BigDecimal via the exclusive absolute limit and
`setScale(12, RoundingMode.UNNECESSARY)`, discarding the result. Negative values may be representable by
this numeric policy; SalesTarget and the SQL constraint independently reject them as configured targets.
The policy has only Java imports and no persistence/framework dependency.

Important boundary: PostgreSQL coerces NUMERIC(30,12) before checking constraints. V9 itself does not
prevent rounding by a direct SQL writer. Future application write paths must call the policy BEFORE insertion.
No registration path exists yet. Decimal scale and scale-sensitive record equality are not guaranteed across storage.

Executed from `backend`:

```powershell
mvn -o '-Dtest=SalesTargetPeriodTest,SalesTargetIdentityTest,SalesTargetTest,SalesTargetStoragePolicyTest,SalesTargetMigrationTest' test
```

Result: **BUILD SUCCESS**, 66 cases, 0 failures, 0 errors, 0 skipped:

| Test class | Cases |
| --- | ---: |
| SalesTargetPeriodTest | 19 |
| SalesTargetIdentityTest | 3 |
| SalesTargetTest | 15 |
| SalesTargetStoragePolicyTest | 22 |
| SalesTargetMigrationTest | 7 |

The original 37 regression cases all passed. Decimal cases cover exact bounds, zero, trailing zeros beyond
scale 12, rounding rejection, scientific notation, null and original-scale preservation.

Docker 28.5.1 was available. The tests used PostgreSQL 16.13 from the existing postgres:16-alpine image,
with JDBC and Flyway 9.22.3, exclusively through Testcontainers credentials and a disposable database.
Testcontainers downloaded its auxiliary cleanup image testcontainers/ryuk:0.12.0 automatically.
No Maven dependencies were installed or downloaded; the POM was not changed.

Migration tests verified V1..V9 on an empty database, a no-op second migration, Flyway validation,
V8-to-V9 upgrade preserving an existing user, generated ID/time, numeric retrieval, both duplicate rules,
independent periods/categories, invalid months/amounts/pairs, overflow/infinity/NaN and required columns.
Hibernate schema validation is deferred until a sales-target entity exists; no artificial JPA entity was created.

Warnings recorded: implicit compilation/annotation processing and Flyway's recommendation to upgrade because
PostgreSQL 16.13 is newer than its tested support (15). These did not prevent successful migrations or tests.
No dependency configuration was changed to silence them.

- Increment 3A: created V9 and the separate storage policy with unit and real PostgreSQL migration tests;
  documented the successful 66-case execution. No migrations against the development database, commits or push.
