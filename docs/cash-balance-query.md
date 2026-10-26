# cashBalanceQuery (sales -> reports)

Publishes the daily cash balance table ("saldo en caja", table 4c of the daily flow) so Reports can read it
without touching the cash, savings fund or expense tables.

## Contract

Public API of the `sales` module, in its root package (every subpackage is internal):

- `sales.CashBalanceQuery#findDailyCashBalance(LocalDate)` returns `Optional<CashBalanceTableInfo>`;
  empty when the date has no journal.
- `sales.CashBalanceTableInfo`: date, journal consecutive ("Planilla No."), journal status, savings fund
  (previous balance, contribution, payments, accumulated), expenses (items and total), cash balance.

The date is the journal date (label of the 06:00-06:00 operating day), not an instant. No REST endpoint,
no migration and no events: it is an in-process query.

## Using it from another module

Inject `CashBalanceQuery` by constructor, like `WorkerService` receives `ShiftExternalService`. Never import
anything from `sales.domain`, `sales.infrastructure`, `sales.api`, `sales.application` or `sales.exception`.
`SalesPublicApiBoundaryTest` enforces it.

## Values still null

`dailyCash`, `dominusAccumulatedBalance` and `difference` are `null` until the sellers' close (step 2) is
published. The only place to change is `CashBalanceQueryService#pendingFields`; the contract does not change.
Pending with the client: formula of "saldo de efectivo en planilla" and "sobrante/faltante", and the
accumulated days of the fund (period 21-20).
