package com.marzouki.shop.invoice;

import com.marzouki.shop.order.Order;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/** Turns an invoice into a PDF: a fixed HTML layout, filled in with escaped values, drawn by OpenHTMLtoPDF. */
@Component
@RequiredArgsConstructor
class InvoiceRenderer {

    /** A font with the characters customers' names and addresses need (it ships inside PDFBox). */
    private static final String FONT = "/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf";

    private final SpringTemplateEngine templates;

    static {
        // the library writes an INFO line per document to the console otherwise
        com.openhtmltopdf.util.XRLog.setLoggingEnabled(false);
    }

    byte[] render(Invoice invoice, Order order, String accent) {
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("invoice", invoice);
        context.setVariable("order", order);
        context.setVariable("amounts", InvoiceAmounts.of(order.getTotal(), invoice.getVatPercent()));
        context.setVariable("accent", accent);
        context.setVariable("cancelled", order.getStatus() == com.marzouki.shop.order.OrderStatus.CANCELLED);
        String html = templates.process("invoice/invoice", context);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useFont(() -> {
                InputStream stream = InvoiceRenderer.class.getResourceAsStream(FONT);
                if (stream == null) {
                    throw new IllegalStateException("The invoice font is missing");
                }
                return stream;
            }, "Invoice");
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not create the invoice PDF", e);
        } catch (Exception e) {
            throw new IllegalStateException("Could not create the invoice PDF", e);
        }
    }
}