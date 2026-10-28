package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RedebanCsvParserTest {

    @Test
    void readsSemicolonFileWithDateTimeAmountAndReference() {
        String csv = "Fecha;Hora;Valor;Referencia\n"
                + "05/08/2026;10:15:30;$ 300.000;A123\n"
                + "05/08/2026;02:30;135.500,00;A124\n";

        List<RedebanTransaction> rows = RedebanCsvParser.parse(csv);

        assertEquals(2, rows.size());
        assertEquals(LocalDate.of(2026, 8, 5), rows.get(0).date());
        assertEquals(LocalTime.of(10, 15, 30), rows.get(0).time());
        assertEquals(new BigDecimal("300000.00"), rows.get(0).amount());
        assertEquals("A123", rows.get(0).reference());
        assertEquals(new BigDecimal("135500.00"), rows.get(1).amount());
        assertEquals(2, rows.get(0).row() );
        assertTrue(rows.get(0).isQr());
    }

    @Test
    void readsCommaFileWhereTheDateColumnAlsoCarriesTheTimeAndBomIsIgnored() {
        String csv = "\uFEFF\"Fecha Transacción\",\"Monto\",\"Tipo\"\n"
                + "\"2026-08-05 14:05:00\",\"200000\",\"QR\"\n"
                + "\"2026-08-05T20:10:00\",\"1.234\",\"Crédito\"\n";

        List<RedebanTransaction> rows = RedebanCsvParser.parse(csv);

        assertEquals(LocalTime.of(14, 5), rows.get(0).time());
        assertEquals(LocalTime.of(20, 10), rows.get(1).time());
        assertEquals(new BigDecimal("1234.00"), rows.get(1).amount());
        assertTrue(rows.get(0).isQr());
        assertFalse(rows.get(1).isQr());
    }

    @Test
    void withoutDateAndTimeColumnsTheValuesAreNull() {
        List<RedebanTransaction> rows = RedebanCsvParser.parse("valor\n240508\n");

        assertNull(rows.get(0).date());
        assertNull(rows.get(0).time());
        assertEquals(new BigDecimal("240508.00"), rows.get(0).amount());
    }

    @Test
    void acceptsAmPmTimes() {
        List<RedebanTransaction> rows = RedebanCsvParser.parse("hora;valor\n2:30 p. m.;1000\n12:05 a. m.;1000\n");

        assertEquals(LocalTime.of(14, 30), rows.get(0).time());
        assertEquals(LocalTime.of(0, 5), rows.get(1).time());
    }

    @Test
    void amountFormats() {
        assertEquals(new BigDecimal("1234567"), RedebanCsvParser.parseAmount("1.234.567", "x"));
        assertEquals(new BigDecimal("1234.50"), RedebanCsvParser.parseAmount("1.234,50", "x"));
        assertEquals(new BigDecimal("1234.50"), RedebanCsvParser.parseAmount("1,234.50", "x"));
        assertEquals(new BigDecimal("1234567.5"), RedebanCsvParser.parseAmount("1234567.5", "x"));
        assertEquals(new BigDecimal("1500"), RedebanCsvParser.parseAmount("$1.500", "x"));
    }

    @Test
    void missingAmountColumnIsRejected() {
        assertThrows(ReconciliationValidationException.class, () -> RedebanCsvParser.parse("fecha;hora\n05/08/2026;10:00\n"));
    }

    @Test
    void badRowStopsTheImportWithItsLineNumber() {
        ReconciliationValidationException error = assertThrows(ReconciliationValidationException.class,
                () -> RedebanCsvParser.parse("fecha;valor\n05/08/2026;1000\n05/08/2026;abc\n"));

        assertTrue(error.getMessage().contains("línea 3"));
    }

    @Test
    void zeroOrNegativeAmountIsRejected() {
        assertThrows(ReconciliationValidationException.class, () -> RedebanCsvParser.parse("valor\n0\n"));
        assertThrows(ReconciliationValidationException.class, () -> RedebanCsvParser.parse("valor\n-500\n"));
    }

    @Test
    void emptyFileIsRejected() {
        assertThrows(ReconciliationValidationException.class, () -> RedebanCsvParser.parse("  \n"));
    }

    @Test
    void statusColumnSeparatesApprovedFromDeclinedRows() {
        List<RedebanTransaction> rows = RedebanCsvParser.parse("Fecha;Hora;Valor;Estado\n"
                + "05/08/2026;10:00;1000;Aprobada\n"
                + "05/08/2026;11:00;2000;Rechazada\n"
                + "05/08/2026;12:00;3000;APPROVED\n"
                + "05/08/2026;13:00;4000;Anulada\n");

        assertTrue(rows.get(0).isApproved());
        assertFalse(rows.get(1).isApproved());
        assertTrue(rows.get(2).isApproved());
        assertFalse(rows.get(3).isApproved());
    }

    @Test
    void withoutAStatusColumnEveryRowCountsAsApproved() {
        assertTrue(RedebanCsvParser.parse("valor\n1000\n").get(0).isApproved());
    }

    @Test
    void invalidFormatUsesTheMessageOfTheStory() {
        ReconciliationValidationException missingColumn = assertThrows(ReconciliationValidationException.class,
                () -> RedebanCsvParser.parse("fecha;hora\n05/08/2026;10:00\n"));
        ReconciliationValidationException empty = assertThrows(ReconciliationValidationException.class,
                () -> RedebanCsvParser.parse(" "));

        assertTrue(missingColumn.getMessage().startsWith("El archivo no tiene el formato esperado"));
        assertTrue(empty.getMessage().startsWith("El archivo no tiene el formato esperado"));
    }

    @Test
    void amountsWithMoreThanTwoDecimalsAreRejected() {
        assertThrows(ReconciliationValidationException.class, () -> RedebanCsvParser.parse("valor\n1000,1234\n"));
    }
}
