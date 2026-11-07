package com.marzouki.shop.invoice;

import com.marzouki.shop.order.OrderService;
import com.marzouki.shop.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Invoice downloads: customers get their own, managers get any. */
@Controller
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoices;
    private final OrderService orders;

    @GetMapping("/account/orders/{id}/invoice.pdf")
    public ResponseEntity<byte[]> mine(@AuthenticationPrincipal AppUserDetails me, @PathVariable Long id) {
        orders.getOwned(id, me.getId());
        return pdf(invoices.document(id));
    }

    @GetMapping("/manage/orders/{id}/invoice.pdf")
    public ResponseEntity<byte[]> any(@PathVariable Long id) {
        orders.get(id);
        return pdf(invoices.document(id));
    }

    public static ResponseEntity<byte[]> pdf(InvoiceService.Document document) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(document.filename()).build().toString())
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(document.pdf());
    }
}