# Savings contribution on journal close (S3-08)

When a journal is closed, the savings fund receives the daily contribution: the fund grows by the `DAILY_SAVINGS`
parameter (250.000 since 2026-01-01) for every closed journal.

## How it is triggered

Closing the journal (step 6, C-08) publishes `JournalClosedEvent` from `JournalService`. `sales` listens to it with
`SavingsContributionOnJournalClosed` (in `sales.application`).

## What the listener does

1. Reads `DAILY_SAVINGS` in force on `event.journalDate()`, so a later change of the parameter does not alter the
   days already closed.
2. Calls `SavingsFundService.registerContributionIfAbsent(date, amount, "Aporte diario al cierre de la jornada")`.
3. If the day already has a contribution (typed by hand, or the event arrived twice) it adds nothing and does not fail.

## Failures

The listener runs asynchronously (`AsyncEventsConfiguration`) and the event stays in `event_publication` until the
listener finishes. If it fails (for example, there is no `DAILY_SAVINGS` for that date), closing the journal is not
affected and the publication stays incomplete. With
`spring.modulith.events.republish-outstanding-events-on-restart=true` it is retried when the application starts again.

## Not covered

Journals closed before this listener existed have no contribution. Register them with
`POST /api/v1/sales/savings-fund/contributions`.
