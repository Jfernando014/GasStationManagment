package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the Redeban CSV (HU-35). Pure Java.
 *
 * <p>Contract (the real export layout still has to be confirmed with a sample file, so the reader is tolerant):
 * <ul>
 *   <li>First line is the header. Separator {@code ;}, {@code ,} or tab is detected; quotes and BOM are handled.</li>
 *   <li>Required column: the amount (valor, monto, importe, total...).</li>
 *   <li>Optional: date (fecha), time (hora) - the date column may also carry the time -, type
 *       (tipo, medio de pago, franquicia...) and reference (referencia, autorizacion, rrn...).</li>
 *   <li>Dates: yyyy-MM-dd, dd/MM/yyyy, dd-MM-yyyy. Times: H:mm[:ss], with optional am/pm.</li>
 *   <li>Amounts: Colombian (1.234.567 or 1.234,50) or plain (1234567.50); a leading $ is ignored.</li>
 * </ul>
 * Any row that cannot be read stops the import with the line number: nothing is skipped silently.
 */
public final class RedebanCsvParser {

    private static final List<String> DATE_HEADERS = List.of("fecha", "fecha transaccion", "fecha de la transaccion",
            "fecha y hora", "fecha hora", "date", "datetime");
    private static final List<String> TIME_HEADERS = List.of("hora", "hora transaccion", "hora de la transaccion",
            "time");
    private static final List<String> AMOUNT_HEADERS = List.of("valor", "monto", "importe", "valor total",
            "valor transaccion", "valor de la transaccion", "amount", "total");
    private static final List<String> TYPE_HEADERS = List.of("tipo", "tipo transaccion", "tipo de transaccion",
            "medio", "medio de pago", "producto", "franquicia", "tipo de pago");
    private static final List<String> STATUS_HEADERS = List.of("estado", "estado transaccion",
            "estado de la transaccion", "status", "respuesta", "resultado", "estatus");
    private static final List<String> REFERENCE_HEADERS = List.of("referencia", "autorizacion",
            "numero de autorizacion", "rrn", "comprobante", "id transaccion", "numero transaccion", "recibo");

    private static final Pattern TIME = Pattern.compile("^(\\d{1,2}):(\\d{2})(?::(\\d{2}))?(am|pm)?$");

    /** Message required by HU-42 #5 when the file is not a Redeban export. */
    public static final String FORMAT_ERROR = "El archivo no tiene el formato esperado";

    private RedebanCsvParser() {
    }

    public static List<RedebanTransaction> parse(String text) {
        if (text == null || text.isBlank()) {
            throw new ReconciliationValidationException("file", FORMAT_ERROR + ": está vacío");
        }
        String content = text.startsWith("\uFEFF") ? text.substring(1) : text;
        List<String> lines = new ArrayList<>();
        List<Integer> lineNumbers = new ArrayList<>();
        String[] raw = content.split("\\r?\\n", -1);
        for (int i = 0; i < raw.length; i++) {
            if (!raw[i].isBlank()) {
                lines.add(raw[i]);
                lineNumbers.add(i + 1);
            }
        }
        char separator = detectSeparator(lines.get(0));
        List<String> header = split(lines.get(0), separator).stream().map(RedebanCsvParser::normalize).toList();
        int dateIndex = indexOf(header, DATE_HEADERS);
        int timeIndex = indexOf(header, TIME_HEADERS);
        int amountIndex = indexOf(header, AMOUNT_HEADERS);
        int typeIndex = indexOf(header, TYPE_HEADERS);
        int referenceIndex = indexOf(header, REFERENCE_HEADERS);
        int statusIndex = indexOf(header, STATUS_HEADERS);
        if (amountIndex < 0) {
            throw new ReconciliationValidationException("file",
                    FORMAT_ERROR + ": falta la columna del valor (valor, monto, importe o total) en la primera línea");
        }

        List<RedebanTransaction> result = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            int row = lineNumbers.get(i);
            List<String> cells = split(lines.get(i), separator);
            String where = "línea " + row;
            BigDecimal amount = parseAmount(cell(cells, amountIndex), where);
            if (amount.signum() <= 0) {
                throw new ReconciliationValidationException("file", "El valor de la " + where + " debe ser mayor que cero");
            }
            if (amount.stripTrailingZeros().scale() > 2) {
                throw new ReconciliationValidationException("file",
                        "El valor de la " + where + " no puede tener más de 2 decimales");
            }
            amount = amount.setScale(2);
            LocalDate date = null;
            LocalTime time = null;
            String dateCell = cell(cells, dateIndex);
            if (dateCell != null) {
                String[] parts = dateCell.strip().split("[T\\s]+", 2);
                date = parseDate(parts[0], where);
                if (parts.length == 2) {
                    time = parseTime(parts[1], where);
                }
            }
            String timeCell = cell(cells, timeIndex);
            if (timeCell != null) {
                time = parseTime(timeCell, where);
            }
            String statusCell = cell(cells, statusIndex);
            result.add(new RedebanTransaction(date, time, amount, cell(cells, typeIndex),
                    cell(cells, referenceIndex), row, statusCell == null ? null : isApprovedStatus(statusCell)));
        }
        return List.copyOf(result);
    }

    /** "Aprobada", "Aprobado", "APPROVED", "Exitosa", "Autorizada", "OK" or "00" are approved; anything else is not. */
    static boolean isApprovedStatus(String status) {
        String s = normalize(status);
        return s.contains("aprob") || s.contains("approved") || s.contains("exitos") || s.contains("autoriz")
                || s.equals("ok") || s.equals("00");
    }

    static BigDecimal parseAmount(String cell, String where) {
        if (cell == null) {
            throw new ReconciliationValidationException("file", "Falta el valor en la " + where);
        }
        String s = cell.replace("$", "").replace(" ", "").strip();
        if (!s.matches("-?[0-9.,]+")) {
            throw new ReconciliationValidationException("file", "El valor '" + cell + "' de la " + where + " no es numérico");
        }
        int dots = count(s, '.');
        int commas = count(s, ',');
        String normalized;
        if (dots > 0 && commas > 0) {
            char decimal = s.lastIndexOf('.') > s.lastIndexOf(',') ? '.' : ',';
            normalized = decimal == '.' ? s.replace(",", "") : s.replace(".", "").replace(',', '.');
        } else if (dots + commas == 0) {
            normalized = s;
        } else {
            char sep = dots > 0 ? '.' : ',';
            int n = dots > 0 ? dots : commas;
            boolean thousands = n > 1 || s.length() - s.lastIndexOf(sep) - 1 == 3;
            normalized = thousands ? s.replace(String.valueOf(sep), "") : s.replace(sep, '.');
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            throw new ReconciliationValidationException("file", "El valor '" + cell + "' de la " + where + " no es numérico");
        }
    }

    private static LocalDate parseDate(String s, String where) {
        try {
            if (s.matches("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}")) {
                String[] p = s.split("[-/]");
                return LocalDate.of(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
            }
            if (s.matches("\\d{1,2}[-/]\\d{1,2}[-/]\\d{4}")) {
                String[] p = s.split("[-/]");
                return LocalDate.of(Integer.parseInt(p[2]), Integer.parseInt(p[1]), Integer.parseInt(p[0]));
            }
        } catch (java.time.DateTimeException | NumberFormatException e) {
            // falls through to the error below
        }
        throw new ReconciliationValidationException("file", "La fecha '" + s + "' de la " + where + " no es válida");
    }

    private static LocalTime parseTime(String s, String where) {
        String t = s.toLowerCase(Locale.ROOT).replace(".", "").replace(" ", "").strip();
        Matcher m = TIME.matcher(t);
        if (m.matches()) {
            int hour = Integer.parseInt(m.group(1));
            int minute = Integer.parseInt(m.group(2));
            int second = m.group(3) == null ? 0 : Integer.parseInt(m.group(3));
            String meridiem = m.group(4);
            if (meridiem != null) {
                if (hour < 1 || hour > 12) {
                    throw new ReconciliationValidationException("file", "La hora '" + s + "' de la " + where + " no es válida");
                }
                hour = hour % 12 + (meridiem.equals("pm") ? 12 : 0);
            }
            if (hour <= 23 && minute <= 59 && second <= 59) {
                return LocalTime.of(hour, minute, second);
            }
        }
        throw new ReconciliationValidationException("file", "La hora '" + s + "' de la " + where + " no es válida");
    }

    private static char detectSeparator(String header) {
        char best = ';';
        int bestCount = -1;
        for (char candidate : new char[]{';', ',', '\t'}) {
            int n = split(header, candidate).size();
            if (n > bestCount) {
                best = candidate;
                bestCount = n;
            }
        }
        return best;
    }

    /** Splits one line honoring double quotes ("" is an escaped quote). */
    static List<String> split(String line, char separator) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == separator) {
                cells.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        cells.add(current.toString());
        return cells;
    }

    private static String cell(List<String> cells, int index) {
        if (index < 0 || index >= cells.size()) {
            return null;
        }
        String value = cells.get(index).strip();
        return value.isEmpty() ? null : value;
    }

    private static int indexOf(List<String> header, List<String> aliases) {
        for (int i = 0; i < header.size(); i++) {
            if (aliases.contains(header.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replace('_', ' ').strip();
    }

    private static int count(String s, char c) {
        return (int) s.chars().filter(x -> x == c).count();
    }
}
