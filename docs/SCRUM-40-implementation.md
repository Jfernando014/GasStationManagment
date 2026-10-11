# SCRUM-40 implementation log

## Scope and status

Baseline inspected: commit `232688e` on `feature/back/nicolas_dev`.

- HU-14: increments 1-3C provide configured targets and authenticated REST persistence; the workbook distribution increment below adds a pure calculation engine, sourced parameter snapshots and ADMIN calculation endpoints. Automatic live-history/schedule retrieval and unconfirmed exceptions remain unavailable.
- HU-16: pending; requires real historical sales from the owning sales module.
- HU-17: pending; requires approved scenario rules and the same real sales source.

Increment 3A baseline: clean Git checkout at `49aae77` on `feature/back/sales-targets-statistics`; V9 was available.
V9 now defines `sales_targets`; it was executed only in a disposable test container.
Increment 3A introduced no Java persistence. Increment 3B starts from a clean checkout at `b49ed92`
on the same branch and adds sales-only Java persistence. The development database and credentials were not used
or modified. V1 through V9 are unchanged in 3B. No endpoints or individual calculations are introduced.

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
| `StoredSalesTarget.java` | Immutable result containing database ID, SalesTarget and database-generated creation instant. Added in increment 3B. |

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
- Additional audit requirements and future schema evolution. Registration HTTP authorization is implemented in 3C.
- Editing lock rules and any future edit/delete operations; neither operation is exposed by the targets API.
- Monthly consistency rules, if any, relative to an annual target.
- Live parameter refresh and operational exceptions. Hourly weights, immutable parameter snapshots and workbook-confirmed titular/support allocation are implemented in the distribution increment below.
- Manual adjustments, holidays, operational date of night shifts and historical-average denominator.
- Public assignment query contract in shifts and public historical-sales contract in sales.

No default percentages, formulas, lock rules, maximum-sales field or individual calculations are implemented.
Units are explicit, never inferred or converted. The domain rejects negative values; the separate storage policy
additionally validates the approved provisional NUMERIC limits before any application insertion.
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

## Increment 3B: Java registration and query persistence

Files created under `backend/src/main/java/com/edu/unicauca/gasstation/backend/sales/`:

- `domain/models/StoredSalesTarget.java`
- `domain/repositories/SalesTargetRepository.java`
- `domain/services/SalesTargetService.java`
- `exception/DuplicateSalesTargetException.java`
- `infrastructure/persistence/entities/SalesTargetEntity.java`
- `infrastructure/persistence/repositories/JpaSalesTargetRepository.java`
- `infrastructure/persistence/mappers/SalesTargetPersistenceMapper.java`
- `infrastructure/persistence/adapters/SalesTargetRepositoryAdapter.java`

Files created under `backend/src/test/java/com/edu/unicauca/gasstation/backend/sales/`:

- `SalesTargetTestData.java` (shared fixture, not a test class)
- `domain/services/SalesTargetServiceTest.java`
- `infrastructure/persistence/SalesTargetPersistenceMapperTest.java`
- `infrastructure/persistence/SalesTargetRepositoryAdapterTest.java`
- `infrastructure/persistence/SalesTargetRepositoryTest.java`

The only existing file modified is this document. No migrations, old domain types, other modules,
environment files, Docker files or dependencies were changed.

Architecture follows workers/shifts/inventory: domain port, separate JPA entity, mapper and adapter.
The Spring Data repository extends the narrow Repository marker rather than JpaRepository to avoid inheriting
update/delete methods. Its internal saveAndFlush operation is used only with fresh entities without an ID.
Domain port and service expose register, findById, findByIdentity and findByYear. Absent single results use Optional;
year queries are ordered by month ascending (annual NULL last in PostgreSQL), then category.
These classes remain internal to sales; this increment does not expose a cross-module query contract.

SalesTargetService validates storage representability before delegating. The adapter repeats that validation
to protect direct port callers. Neither validation alters the original BigDecimal.
All registration operations are transactional; query operations are read-only.
The mapper creates an entity without ID or creation time. JPA omits created_at in INSERT; PostgreSQL applies
V9's CURRENT_TIMESTAMP default. After saveAndFlush, EntityManager.refresh retrieves creation time and the
stored numeric representation. StoredSalesTarget returns this metadata with a reconstructed pure SalesTarget.
All mapped business fields are updatable=false; no editing or deletion feature is implemented.

Both partial unique indexes remain the sole authoritative duplicate protection, including concurrent inserts.
The adapter translates only Hibernate ConstraintViolationException with SQLSTATE 23505 AND the exact name
uq_sales_targets_annual or uq_sales_targets_monthly into DuplicateSalesTargetException, retaining the cause.
Other uniqueness/check/unknown integrity errors are rethrown unchanged. No message substring matching or
pre-insertion existence check is used. A duplicate rolls back the registration transaction.

Numeric comparisons after persistence use BigDecimal.compareTo; record equality must not be assumed across
scale changes. Original in-memory domain values preserve scale. Fixed PostgreSQL scale is accepted on retrieval.

Executed from `backend`, with cached dependencies and disposable PostgreSQL 16.13 containers:

```powershell
mvn -o '-Dtest=SalesTarget*Test' test
```

The first run executed 84 cases with 0 failures, 1 error and 0 skipped: repeated Mockito stubbing invoked an
already configured exception in SalesTargetRepositoryAdapterTest. Corrected the test to use doThrow stubbing.
No production behavior needed changing. Re-ran the full SCRUM-40 selection.

Final result: **BUILD SUCCESS**, 84 cases, 0 failures, 0 errors, 0 skipped.

| Test class | Cases |
| --- | ---: |
| SalesTargetPeriodTest | 19 |
| SalesTargetIdentityTest | 3 |
| SalesTargetTest | 15 |
| SalesTargetStoragePolicyTest | 22 |
| SalesTargetMigrationTest | 7 |
| SalesTargetServiceTest | 6 |
| SalesTargetPersistenceMapperTest | 1 |
| SalesTargetRepositoryAdapterTest | 6 |
| SalesTargetRepositoryTest | 5 |

All 66 previous cases passed, plus 18 new cases. The integration suite explicitly uses ddl-auto=validate,
never create-drop, and Flyway V1..V9 against Testcontainers connection details. Hibernate 6.4.4 validated the
mapped schema successfully. It verifies registration and readback, database timestamp, annual/monthly NULL
queries, empty results, basket zero, exact boundaries, rejected values without inserts, duplicate rollback
and concurrent annual/monthly registration (one success and one domain duplicate per pair).
The integration test runs transactions per service call; it does not wrap the entire test in a rollback transaction.
No production initialization runners or local development datasource were used.

Existing Flyway/PostgreSQL support warning remains. Java 21 also reports Mockito/Byte Buddy dynamic-agent
and class-sharing warnings; the initial compilation reported implicit annotation-processing warnings.
Expected duplicate tests produce PostgreSQL error logs while their assertions pass. No dependency settings
were changed to suppress these messages.

Limitations: no REST authorization/API, no editing/delete permissions at database level, no cross-module facade,
no real sales-history source, no maximum sales or individual calculations. Direct SQL writers can bypass the
Java representability policy; this existing boundary remains unchanged. Changing these requires a later scope.

- Increment 3B: implemented registration/query persistence; validated the real V9 mapping with PostgreSQL,
  Flyway and Hibernate validate, and confirmed all 84 selected cases passed after correcting one test mock.
  No commits or push.

## Increment 3C: REST registration and queries

Baseline verified: clean Git checkout at `2dcf6aa`, branch `feature/back/sales-targets-statistics`.
The application is intended for a single administrator. Only sales sources, sales tests and this document
were changed. Security, other modules, V1-V9, dependencies, Docker and configuration files are unchanged.

### Files

Production files under `backend/src/main/java/com/edu/unicauca/gasstation/backend/`:

- `sales/api/SalesTargetController.java`: the four REST operations and explicit ADMIN registration guard.
- `sales/api/dto/CreateSalesTargetRequest.java`: year, optional month, category, BigDecimal value and unit;
  Jakarta validation. ID and createdAt are not request DTO fields and cannot affect generated metadata.
- `sales/api/dto/SalesTargetResponse.java`: immutable HTTP response with database ID and creation Instant.
- `sales/infrastructure/mappers/SalesTargetApiMapper.java`: HTTP/domain conversion without rounding or normalization.
- `sales/api/SalesTargetExceptionHandler.java`: ProblemDetail handling scoped exclusively to this controller.
- `sales/exception/SalesTargetNotFoundException.java`: absent individual query result.

Tests under `backend/src/test/java/com/edu/unicauca/gasstation/backend/`:

- `sales/api/SalesTargetControllerTest.java`: 25 MVC and local-guard cases.
- `sales/api/SalesTargetApiIntegrationTest.java`: 2 servlet HTTP/PostgreSQL integration cases.
- `sales/infrastructure/mappers/SalesTargetApiMapperTest.java`: 2 mapper cases.

### HTTP contract

| Method and route | Behavior |
| --- | --- |
| POST `/api/v1/sales/targets` | ADMIN only; 201, response body and Location pointing to the generated ID. |
| GET `/api/v1/sales/targets/{id}` | 200 for a stored target, 404 if absent. |
| GET `/api/v1/sales/targets/by-identity?year=2026&category=FUEL_AGGREGATE` | Exact annual lookup; omitted month means annual, not all months. |
| GET `/api/v1/sales/targets/by-identity?year=2026&month=8&category=BASKET` | Exact monthly lookup; 404 if absent. |
| GET `/api/v1/sales/targets?year=2026` | 200 with annual/monthly targets, or [] for an empty year. |

All GET routes retain authentication from the existing global security chain. The controller delegates all
registration and queries to SalesTargetService within the same sales module. No cross-module domain access
or direct persistence access is introduced by the controller. No PUT, PATCH or DELETE is exposed.
Month 0/13, empty/blank month and nonnumeric month are rejected. No new business year limits are imposed.

Monthly fuel example (example data, not a default):

```json
{"year":2026,"month":8,"category":"FUEL_AGGREGATE","value":26920,"unit":"GALLONS"}
```

Annual basket example:

```json
{"year":2026,"month":null,"category":"BASKET","value":8400000,"unit":"COP"}
```

Response fields are `id`, `year`, nullable `month`, `category`, `value`, `unit`, `createdAt`.
createdAt is the database-generated Instant serialized as ISO-8601. Amounts are JSON numbers bound directly
to BigDecimal. Clients must avoid lossy floating-point conversion. Original domain scale is preserved during
HTTP mapping; after PostgreSQL retrieval only numeric equality is guaranteed, as approved in 3A.
Zero remains accepted. Category/unit invariants remain in SalesTarget. Exact representability remains enforced
by SalesTargetService and the persistence adapter; no @Digits annotation rejects otherwise valid extra zeros.
There are no individual calculations, automatic annual division, maximum-sales fields or edit locks.

### Security and errors

The sole targets POST method calls requireAdmin(Authentication) before mapping or invoking register.
It rejects null, unauthenticated and anonymous authentication, even an anonymous token carrying ADMIN;
authenticated principals need the exact authority ADMIN. ROLE_ADMIN is rejected. No client role is read.
There is no @PreAuthorize dependency, no new filter, no copied JWT implementation and no security configuration change.
MVC tests inspect the registered routes: exactly one targets POST maps to this guarded method, with no edit/delete routes.
Existing journal POST operations are separate and do not call SalesTargetService or register targets.

With the real existing security chain and JWT filter active, valid ADMIN requests succeed; authenticated
READ_ONLY/ROLE_ADMIN requests produce 403 ProblemDetail and no service call. Anonymous POST and GET requests
produce 403 from the unchanged global security chain and no service call. This is the actual observed behavior,
not a promise of 401. The local missing-authentication exception maps to 401 only if execution reaches the controller.

Sales errors use application/problem+json. Bean validation is 400 with code VALIDATION_ERROR and field errors;
domain/storage argument errors are 400 INVALID_SALES_TARGET; specific duplicates are 409 DUPLICATE_SALES_TARGET;
individual misses are 404 SALES_TARGET_NOT_FOUND; local permission failures are 403 ACCESS_DENIED.
Malformed JSON, missing query parameters and type mismatches use inherited ResponseEntityExceptionHandler
ProblemDetail responses (400). Unclassified database integrity failures are not caught or mislabeled as duplicates
or validation errors; a dedicated MVC test confirms they propagate.

JWT parsing/expiry failures in the existing global filter remain outside the sales advice. Invalid or expired
JWT behavior was not tested in 3C and is not asserted to always return 401. Login/password verification is also
not part of these tests: they use real generated JWTs and the real filter/chain, with a mocked UserDetailsService
and AuthenticationProvider. No new production role was added for permission-denial tests.

### Execution and results

Executed offline from backend, using existing dependencies and disposable PostgreSQL 16 containers:

```powershell
mvn -o '-Dtest=SalesTarget*Test' test
```

Final result: **BUILD SUCCESS**, **113 tests, 0 failures, 0 errors, 0 skipped**.
Existing regression suite: 84 cases. New cases: 25 controller/security, 2 HTTP integration, 2 mapper = 29.
The individual existing suite counts remain 19 + 3 + 15 + 22 + 7 + 6 + 1 + 6 + 5 = 84.
Results were confirmed from existing Surefire XML/text reports in backend/target/surefire-reports.

Coverage includes ADMIN 201/Location/metadata; non-ADMIN and anonymous denials without registration calls;
local null/anonymous/unauthenticated defenses; route inventory; malformed/invalid bodies, null fields,
negative values, incorrect units, months, query parameters, duplicate/missing results; decimal scale and
representability. Integration uses real controller/service/JPA with MockMvc, active security, PostgreSQL
16.13, Flyway V1-V9 and Hibernate ddl-auto=validate. It checks both duplicate indexes, annual/monthly queries,
numeric equality on retrieval, no inserted rows for rejected decimals, and acceptance of zero COP.
It is servlet HTTP integration, not a live TCP server or an end-to-end login test. The development database
was not accessed and no migrations were run against it.

Earlier runs passed 109 and then 113 cases but emitted Hikari shutdown warnings followed by Surefire's
`Surefire is going to kill self fork JVM. The exit has elapsed 30 seconds after System.exit(0).`
The new HTTP integration test now closes its context with @DirtiesContext(AFTER_CLASS), so resources close
before its container is removed. The final full 113-case rerun completed normally in 34.868 seconds without
that shutdown message. No tests failed or were skipped in any of these runs.

The existing Flyway warning persists:

```text
Flyway upgrade recommended: PostgreSQL 16.13 is newer than this version of Flyway and support has not been tested. The latest supported version of PostgreSQL is 15.
```

Mockito/Java dynamic-agent and class-sharing warnings persist; initial compilation also reported implicit
annotation-processing warnings. Expected unique-constraint SQL errors appear in duplicate test logs.
No dependencies were installed or security settings changed to suppress warnings.

- Increment 3C: completed target registration/query REST API within sales, validated ADMIN protection with
  active existing filters and all 113 selected tests. Business decisions listed above remain pending.
  No commit or push.

## Workbook distribution increment — 2026-10-11

### Baseline and primary evidence

Started from clean Git on `feature/back/sales-targets-statistics`, HEAD `d52cbbd`.
The requested `docs/references/modelo_eds_bellavista.xlsx` does not exist. The original 11-sheet workbook
was found at `docs/references/6 OBJETIVOS VENDEDORES BELLAVISTA AGOSTO 2026 (2).xlsx`.
Python/openpyxl were unavailable. Its ZIP/XML content, shared strings, formulas, shared-formula masters and
cached results were read directly with PowerShell/.NET, without installing dependencies or editing the file.
SHA-256 before and after implementation:
`3A4BBD81E2254DD61FEDC73A568DDFED29C879BD53997E437EF72AE9CABD7555`.

The workbook sheets are MODELO NUEVO, BASE DE DATOS, ESCENARIOS, PROM DIA, VENDEDORES, PRESUPUESTO 2026,
HISTÓRICO DE VENTAS, PARAMETROS, CALC METAS, ESQUEMA TURNOS and DASHBOARD.
This implementation follows formula dependencies, not just cached values; it is not a general Excel evaluator.

| Evidence | Verified behavior implemented |
| --- | --- |
| PARAMETROS B5:B28, E4=`SUM(B5:B28)` | 24 relative hourly weights; workbook sum is 158.1, not 100. No fixed required sum other than positive. |
| PARAMETROS B32:B38 | Station historical gallon averages by weekday, explicitly described as a report snapshot through July 2026. |
| CALC METAS D11/E11/F11 | Look up hour/weekday; expected hourly mass is `hourWeight / sumHourWeights * weekdayHistoricalGallons`. |
| PARAMETROS B45:L68 and ESQUEMA TURNOS C5:AG8 | Working-hour masks for each supplied worker/date shift. Night covers 0..5 and 18..23 on the worksheet date. |
| PARAMETROS B71/B72/A74/A75; CALC METAS T11:W11 | Official active-worker weights: titular 0.7, support 0.3 for listed support shift codes, normalized over workers active in that hour. The note attributes the decision to the administrator on 20-Aug-2026. |
| PARAMETROS B79/B80 and CALC METAS X11:AA11/AB11 | Reference active-worker weights 0.5/0.5, separate from official targets. |
| CALC METAS K11 and B4:C7 | Hourly expected mass times worker weight divided by active-weight sum; sum across month; normalize each worker by the total covered mass. |
| CALC METAS D4/E4; MODELO NUEVO AJ14/AJ29 | Apply the same resulting share to the independently configured monthly fuel and basket targets. |
| ESCENARIOS E2:E8 | Average nonzero station daily totals by weekday. HistoricalDayAverages exposes exact fractions and absence, not fabricated zero baselines. |
| BASE DE DATOS F2/G2, PROM DIA, VENDEDORES | Manual arithmetic/adjustments, support cutoffs and performance metrics exist. These were inspected but not automatically imported or generalized as approved sales rules. |
| PRESUPUESTO 2026 C5:S14 and HISTÓRICO DE VENTAS | Monthly/product budgets, sums and execution exist; no formula authorizes dividing an annual target by twelve. Product-specific storage remains outside scope. |

### Implemented formulas and precision

Let `w[h]` be the hourly curve, `a[d]` the weekday historical gallon baseline, and `p[i]` the configured
official/reference role weight for a worker's supplied shift code. For workers active in a particular hour:

```text
expected[d,h] = a[d] * w[h] / SUM(w)
contribution[d,h,i] = expected[d,h] * p[i] / SUM(active p)
workerShare[i] = SUM(contribution[d,h,i]) / SUM(all covered contributions)
individualMonthlyTarget[i] = configuredMonthlyTarget * workerShare[i]
```

The engine uses equivalent exact masses `a[d] * w[h] * p[i] / SUM(active p)` because the common SUM(w)
cancels during final normalization. All intermediate operations use reduced BigInteger fractions, not
double, MathContext or rounded BigDecimal division. A sole active worker receives 100%; two active workers
with configured 0.7/0.3 receive that ratio; multiple active workers are normalized exactly as K11 does.
Support-code classification uses exact code membership rather than Excel SEARCH substring matching.
No 70/30 or 50/50 production defaults are installed: all shares and support codes must be supplied in a profile.
The formula's activation by active coverage and support-code classification is verified in the workbook.

Finite output precision is explicit per request (`outputScale`, 0..12). The total must be exactly representable
at that scale; otherwise calculation fails. Each ideal date/hour/worker quota is floored in that unit, and
the remaining units go to the largest exact fractional remainders. Equal remainders are resolved by the
canonical date, hour, then Java UUID order. This technical apportionment policy was chosen for exact conservation
under the user's explicit authorization; it is not a rounding rule found in Excel. It preserves the original
domain target and conserves the full monthly amount for both official and reference results, including repeating
fractions. It does not imply an approved final display scale for gallons or COP.

Uncovered positive-weight hours are returned explicitly. As in the workbook's zero-contribution/normalization
formulas, they do not contribute to the denominator: the whole target is normalized over covered hours.
This may increase targets on covered hours and must not be mistaken for complete schedule coverage.
No covered positive mass, zero active share sum, incomplete profiles, invalid hours, duplicate worker/date rows,
out-of-month assignments, negative parameters and annual calculation inputs are rejected. Annual registration
continues to work; annual-to-monthly calculation is not implemented. A zero target with usable coverage is valid.

### Architecture, persistence and endpoints

The domain engine is independent of JPA, HTTP, Spring and other modules. It accepts immutable typed inputs and
returns official/reference totals by month, date, hour and worker, individual hourly allocations, precision
metadata, parameter provenance and uncovered-hour diagnostics. SalesDistributionService coordinates the
stored target/profile, the public-contract coverage resolver and the pure engine.

V10 availability was checked immediately before creating `V10__create_sales_distribution_profiles.sql`.
It creates only sales-owned profile, hour-weight, weekday-baseline and support-code tables. Profile ID acts
as an immutable snapshot version, with a DB-generated creation timestamp and mandatory source description.
There is no automatic refresh or seed. JDBC registration is transactional, reads stored numeric values back,
and exposes only registration and lookup. NUMERIC representability is checked before writes through the
immutable validated profile; DB constraints reject invalid ranges, shares and NaN weights. Composite primary
keys prevent duplicate profile/hour, profile/weekday and profile/code entries. The application enforces all
24 hours, all seven days and positive sums; SQL alone does not enforce child-row completeness. SQL permissions
do not forbid direct UPDATE/DELETE, and direct SQL could bypass representability checks, as with V9.
Existing V1-V9 are unchanged. No sales-history, assignment or calculation-run table was created.

| Endpoint | Behavior |
| --- | --- |
| POST `/api/v1/sales/distribution-profiles` | ADMIN; validate and register an explicit complete profile; 201, body and Location. |
| GET `/api/v1/sales/distribution-profiles/{id}` | Authenticated; read profile snapshot; 200 or 404. |
| POST `/api/v1/sales/targets/{id}/distribution` | ADMIN; calculate the existing monthly target with a profile and explicitly supplied assignments; 200, 400 for invalid/unavailable inputs, 404 for missing target/profile. Does not persist results. |

SalesAdminAuthorization shares the same explicit literal ADMIN guard across the original target registration
and both new POST routes. Anonymous, unauthenticated or non-ADMIN principals cannot invoke writes/calculation.
The existing global chain still returns 403 for anonymous HTTP requests. @Valid still precedes the local guard.
Security/JWT classes were neither copied nor modified. Sales advice now covers both sales controllers.

Example profile from the workbook (example/snapshot data, never constants/defaults):

```json
{
  "name": "Bellavista July-2026 report snapshot",
  "source": "Administrator workbook PARAMETROS B5:B38/B71:B80, report through July 2026",
  "hourlyWeights": [1.1,0.9,0.5,0.4,0.5,1.85,5.3,6.6,7,8.1,8.05,9,9.3,9.6,9.15,9.6,9.2,9.85,13.3,12.7,10.9,8.2,4.9,2.1],
  "historicalDailyGallons": {"MONDAY":701.54,"TUESDAY":705.34,"WEDNESDAY":764.49,"THURSDAY":756.2,"FRIDAY":799.65,"SATURDAY":883.42,"SUNDAY":625.86},
  "supportShiftCodes": ["12-7","6-9y5-9","2-9pm","6-9am","2-6pm","6-9y2-6"],
  "titularShare": 0.7, "supportShare": 0.3,
  "referenceTitularShare": 0.5, "referenceSupportShare": 0.5
}
```

Calculation input shape (IDs and rows must be supplied from actual available data; no fabricated production IDs):

```text
profileId: stored snapshot ID
outputScale: explicit integer 0..12
assignments: [{date: worksheet/calendar date, workerId: actual UUID, shiftCode: public catalog code}, ...]
```

Response includes targetId/profileId, the target, decimal policy, parameterSource, assignmentSource,
monthlyTotal, allocations, byDate, structured byHour entries, byWorker and uncoveredHours. Official and reference
totals are kept separate. Consumers must use decimal-safe parsing of JSON amounts. The assignmentSource is
always `EXPLICIT_CALLER_SUPPLIED_WORKSHEET_DATE_ASSIGNMENTS`; this endpoint does not claim to fetch schedules.

### Available contracts and isolated dependencies

PublicShiftCoverageResolver calls only `shifts.ShiftExternalService.getActiveCatalog()` and
`shifts.ScheduleWorkerDirectory.getWorkersByIds()`. The latter is implemented by workers. It verifies existence,
current active status and worker/shift role agreement, and expands public segments, including overnight and split
segments. No internal worker/shift repositories or services are imported. Currently inactive workers/codes cannot
be used through this resolver; there is no historical directory snapshot available through these contracts.
Night segments are projected onto the same worksheet date, matching PARAMETROS, not automatically attributed
to the next operational day. Operational night-date rules remain unapproved.

There is no public assignment-query contract in shifts. SalesAssignmentReadPort records the required future
boundary, without an implementation or fake bean. Existing sales has journals/steps and targets but no usable
historical sales persistence. SalesHistoryReadPort likewise has no production implementation. HistoricalDayAverages
can consume that port using exact arithmetic; its tests use explicitly synthetic daily-total fixtures and mark
missing weekdays unavailable. It is not automatically used to refresh stored workbook baselines. Negative daily
totals are blocked pending approved adjustment rules rather than silently treated as valid weights.

Persisted historicalDailyGallons are parameter snapshots, not a new sales-history source. They are not raw sales,
are not computed automatically from database history, and must not be presented as current actual sales. No workbook
sales adjustments or personal names were imported into production tables. No live schedule/history connection or
HU-16/HU-17 completion is claimed. Maximum sales, manual adjustments, holiday overrides, product-specific capacity,
parameter effective dates, final display precision and persisted calculation-run approval remain pending.

### Files created and modified

New production Java files under `backend/src/main/java/com/edu/unicauca/gasstation/backend/`:

```text
sales/api/SalesAdminAuthorization.java
sales/api/SalesDistributionController.java
sales/api/dto/CalculateDistributionRequest.java
sales/api/dto/CreateDistributionProfileRequest.java
sales/api/dto/SalesDistributionResponse.java
sales/application/SalesDistributionService.java
sales/domain/calculation/ConservingAllocation.java
sales/domain/calculation/DistributionCalculation.java
sales/domain/calculation/DistributionProfile.java
sales/domain/calculation/ExactFraction.java
sales/domain/calculation/HistoricalDayAverages.java
sales/domain/calculation/SalesTargetDistributionEngine.java
sales/domain/repositories/DistributionProfileRepository.java
sales/domain/repositories/SalesAssignmentReadPort.java
sales/domain/repositories/SalesHistoryReadPort.java
sales/infrastructure/adapters/PublicShiftCoverageResolver.java
sales/infrastructure/persistence/adapters/DistributionProfileRepositoryAdapter.java
```

New migration: `backend/src/main/resources/db/migration/V10__create_sales_distribution_profiles.sql`.
New test files under `backend/src/test/java/com/edu/unicauca/gasstation/backend/`:

```text
sales/SalesTargetDistributionTestData.java
sales/domain/services/SalesTargetDistributionEngineTest.java
sales/domain/services/SalesTargetHistoricalDayAveragesTest.java
sales/infrastructure/SalesTargetPublicShiftCoverageResolverTest.java
```

Modified: sales/api/SalesTargetController.java (shared guard extraction), sales/api/SalesTargetExceptionHandler.java
(scope includes new controller), sales/api/SalesTargetApiIntegrationTest.java (four new integration cases),
sales/infrastructure/persistence/SalesTargetMigrationTest.java (latest version 10; upgrade from V8 executes two
migrations), and this document. Test paths in this sentence are relative to the test Java root.

### Actual verification

Executed offline from backend using existing dependencies and cached Docker images:

```powershell
mvn -o '-Dtest=SalesTargetDistributionEngineTest' test
mvn -o '-Dtest=SalesTarget*Test' test
```

The first focused run passed 9 cases. Subsequent full runs passed 127, 132 and finally **133 tests** as coverage
and the HTTP projection were completed. Final result: **BUILD SUCCESS, 133 tests, 0 failures, 0 errors, 0 skipped**
in 40.803 seconds. The prior 113 cases plus 20 additional cases passed. No failing test occurred in this increment.
Surefire XML/text reports were read to verify the actual totals; no hypothetical success is reported.

- SalesTargetDistributionEngineTest: 11 cases, including exact thirds/tiny budgets, official/reference shares,
  single-worker normalization, weekday/hour proportionality, original workbook comparison, both target categories,
  conservation at all result levels, deterministic ordering, absent coverage, leap February, zero, invalid
  shares/weights/hours/precision/periods, scientific notation and duplicate assignments.
- SalesTargetHistoricalDayAveragesTest: 2 cases for explicitly synthetic port fixtures, exclusion of zero,
  missing weekdays, exact 4/3, invalid ranges, duplicates/out-of-range/negative source data.
- SalesTargetPublicShiftCoverageResolverTest: 3 cases for night/split/rest coverage, missing/inactive workers,
  unknown shifts, role mismatch and malformed segments, using only mocked public contracts.
- SalesTargetApiIntegrationTest: 6 cases total (2 prior + 4 new), with real HTTP controller/application service,
  JDBC profile persistence and JPA target persistence, PostgreSQL 16.13, Flyway V1-V10, Hibernate validate and
  active real JWT/security chain. The public worker/catalog contracts and authentication directory remain mocks.
  Checks full profile round-trip, DB timestamp, separate snapshot IDs, SQL constraints/NaN, no rows for invalid
  decimals/configuration, structured results, precise amounts and ADMIN protection of both new POST routes.
- All other prior SCRUM-40 test classes passed. No tests from other modules were selected.

Original-workbook verification uses PARAMETROS data and exact ESQUEMA TURNOS rows, mapped to fixture UUIDs, and
compares against cached C4:C7 shares (0.2754425694, 0.2693149571, 0.2726738509, 0.1825686227) and H4=0.2573270561.
Tolerance is 0.00001 gallons because cached percentages are finite precision; domain output independently
conserves the target exactly. Test fixture IDs are not actual worker IDs, and test results are not real sales.

All migrations executed only in disposable Testcontainers databases, never development PostgreSQL. No credentials,
Docker files, dependencies, security or teammate modules were modified. Existing Flyway/PostgreSQL support,
Mockito dynamic-agent and initial annotation-processing warnings remain; expected SQL constraint errors appear
in negative test logs. No commits or push were performed.

- 2026-10-11: implemented the verified workbook distribution core and explicit-input REST workflow, immutable
  profile persistence in V10, public-contract validation and missing-source ports; all 133 selected tests passed.
