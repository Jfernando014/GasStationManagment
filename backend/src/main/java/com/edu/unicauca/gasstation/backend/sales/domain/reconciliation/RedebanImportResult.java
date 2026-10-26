package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

/**
 * What happened to the rows of an imported Redeban file (HU-42 #1).
 *
 * @param totalRows           data rows read from the file
 * @param accepted            approved QR rows of the operative day: the ones that were summed
 * @param ignoredNotQr        rows whose type is not QR
 * @param ignoredNotApproved  QR rows that were not approved
 * @param ignoredOtherDay     approved QR rows that belong to another operative day
 */
public record RedebanImportResult(QrReconciliationResult reconciliation, int totalRows, int accepted,
                                  int ignoredNotQr, int ignoredNotApproved, int ignoredOtherDay) {
}
