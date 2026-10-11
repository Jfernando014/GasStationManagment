# SCRUM-40 implementation log

## Scope and status

Baseline inspected: commit `232688e` on `feature/back/nicolas_dev`.

- HU-14: first controlled increment implements only infrastructure-independent domain vocabulary and unit tests.
- HU-16: pending; requires real historical sales from the owning sales module.
- HU-17: pending; requires approved scenario rules and the same real sales source.

This increment does not register target amounts. It creates no Flyway migrations, tables, JPA entities,
repositories, endpoints, calculation services or changes to other modules. Existing migrations remain V1 through V8.

## Approved decisions

- Configured targets are distinct from calculated individual targets.
- Annual and monthly targets are independent. Annual targets are not automatically divided into twelve months.
- Valid calendar months are 1 through 12. An absent month represents an annual period.
- Aggregate categories are fuel and basket. Individual fuel products are MOTOR, DIESEL, MAXPRO and EXTRA.
- Hourly weights, shifts and titular/support distribution belong to a later increment.
- Business analysis and Excel formulas are coordinated outside Codex.
- Later historical queries must use the actual sales source rather than introduce a parallel source.
- Cross-module shift access must use public contracts, not internal repositories.

## Domain model

All production files below are under
`backend/src/main/java/com/edu/unicauca/gasstation/backend/sales/domain/models/`.

| File | Responsibility |
| --- | --- |
| `SalesTargetPeriod.java` | Immutable year and optional month, annual/monthly factories and calendar-month validation. No business year limits. |
| `SalesTargetCategory.java` | Aggregate categories `FUEL_AGGREGATE` and `BASKET`. |
| `FuelProduct.java` | Separate Excel product vocabulary. Does not introduce product-specific targets. |
| `SalesTargetIdentity.java` | Immutable conceptual period/category identity; requires both components. Not a final database key. |

The classes use only Java types, with no Spring, JPA, Lombok or other-module imports.
The period representation uses a nullable `Integer` month; this is an in-memory choice, not a database design.
Record equality distinguishes categories, years and months. Required identity components are structural requirements,
not amount validation or editing policies.

`MAXPRO` preserves the Excel terminology. Inventory currently names its enum value `MAX_PRO`.
No conversion or dependency on inventory is introduced in this increment.

## Pending decisions

- Meaning, units and application of maximum sales.
- Target units, amount ranges, decimal precision and rounding.
- Whether configured fuel targets will be broken down by individual product.
- Final persistence model, uniqueness, identifiers and audit requirements.
- Editing lock rules, permitted operations and authorized users.
- Monthly consistency rules, if any, relative to an annual target.
- Hourly weights, parameter versions, titular/support split applicability and individual results.
- Manual adjustments, holidays, operational date of night shifts and historical-average denominator.
- Public assignment query contract in shifts and public historical-sales contract in sales.

No default percentages, formulas, units, lock rules or target-value restrictions are implemented.

## Tests

Test files under `backend/src/test/java/com/edu/unicauca/gasstation/backend/sales/domain/models/`:

- `SalesTargetPeriodTest.java`: annual/monthly distinction, all twelve months, invalid months and constructor validation.
- `SalesTargetIdentityTest.java`: identity distinctions, required components and separation of categories/products.

Executed from `backend` with Java 21 and Maven 3.9.11:

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

## Change log

- 2026-10-10: created this document, four pure Java domain types and two JUnit test classes within the authorized scope.
  No persistence or business calculation was introduced.
- 2026-10-10: executed the selected tests offline successfully and recorded the real results above.
