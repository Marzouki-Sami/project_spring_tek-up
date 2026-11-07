package com.marzouki.shop.export;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Writes RFC 4180 CSV that opens correctly in Excel (UTF-8 with a byte order mark, CRLF line ends). Text
 * that a spreadsheet would treat as a formula, such as a customer name that starts with "=", is neutralised
 * so an export can never run something on the machine of whoever opens it.
 */
final class CsvWriter {

    private static final String FORMULA_STARTS = "=+-@\t\r";

    private final Writer out;

    CsvWriter(Writer out) throws IOException {
        this.out = out;
        out.write('\uFEFF');
    }

    void row(Object... cells) throws IOException {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                out.write(',');
            }
            out.write(cell(cells[i]));
        }
        out.write("\r\n");
    }

    static String cell(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal number) {
            return number.toPlainString();
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Instant instant) {
            return instant.toString();
        }
        String text = value instanceof Enum<?> e ? e.name() : value.toString();
        if (!text.isEmpty() && FORMULA_STARTS.indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        boolean quote = text.indexOf(',') >= 0 || text.indexOf('"') >= 0 || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0;
        return quote ? '"' + text.replace("\"", "\"\"") + '"' : text;
    }
}