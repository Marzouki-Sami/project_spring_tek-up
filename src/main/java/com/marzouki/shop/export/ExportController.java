package com.marzouki.shop.export;

import com.marzouki.shop.audit.AuditService;
import com.marzouki.shop.common.ListQuery;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * CSV downloads. Exports contain personal data, so each one is recorded in the audit log, and the response is
 * never cached.
 */
@Controller
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exports;
    private final AuditService audit;

    @FunctionalInterface
    private interface Body {
        void write(Writer out) throws IOException;
    }

    @GetMapping("/manage/orders/export.csv")
    public void orders(@ModelAttribute("query") ListQuery query, HttpServletResponse response) throws IOException {
        send(response, "orders", "Orders export " + query.params(), out -> exports.orders(query, out));
    }

    @GetMapping("/manage/products/export.csv")
    public void products(@ModelAttribute("query") ListQuery query, HttpServletResponse response) throws IOException {
        send(response, "products", "Products export " + query.params(), out -> exports.products(query, out));
    }

    @GetMapping("/manage/customers/export.csv")
    public void customers(@ModelAttribute("query") ListQuery query, HttpServletResponse response) throws IOException {
        send(response, "customers", "Customers export " + query.params(), out -> exports.customers(query, out));
    }

    @GetMapping("/manage/coupons/export.csv")
    public void coupons(@ModelAttribute("query") ListQuery query, HttpServletResponse response) throws IOException {
        send(response, "coupons", "Coupons export " + query.params(), out -> exports.coupons(query, out));
    }

    @GetMapping("/manage/stock/export.csv")
    public void stock(@RequestParam(defaultValue = "") String from, @RequestParam(defaultValue = "") String to,
                      HttpServletResponse response) throws IOException {
        send(response, "stock-history", "Stock history export " + from + ".." + to, out -> exports.stock(date(from), date(to), out));
    }

    @GetMapping("/admin/audit/export.csv")
    public void audit(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String outcome,
                      @RequestParam(defaultValue = "") String from, @RequestParam(defaultValue = "") String to,
                      HttpServletResponse response) throws IOException {
        send(response, "audit-log", "Audit log export", out -> exports.audit(q, outcome, date(from), date(to), out));
    }

    /** Checks the export can be made before the download starts, so a refusal is a normal error page, not a broken file. */
    private void send(HttpServletResponse response, String name, String what, Body body) throws IOException {
        java.io.StringWriter buffer = new java.io.StringWriter();
        body.write(buffer);
        audit.record("export." + name, "export", null, what);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(name + "-" + LocalDate.now() + ".csv", StandardCharsets.UTF_8).build().toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().cachePrivate().getHeaderValue());
        response.getWriter().write(buffer.toString());
    }

    private static LocalDate date(String text) {
        try {
            return text == null || text.isBlank() ? null : LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}