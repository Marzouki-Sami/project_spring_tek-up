package com.marzouki.shop.api;

import com.marzouki.shop.api.dto.CatalogDtos.PageResponse;
import com.marzouki.shop.api.dto.OrderDtos.OrderRequest;
import com.marzouki.shop.api.dto.OrderDtos.OrderResponse;
import com.marzouki.shop.api.dto.OrderDtos.OrderSummary;
import com.marzouki.shop.api.dto.OrderDtos.StatusRequest;
import com.marzouki.shop.common.ListQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Place and follow your own orders. Managers can list and update every order.")
public class OrderApiController {

    private static final Pattern KEY = Pattern.compile("[A-Za-z0-9_-]{8,64}");

    private final OrderApiService orders;
    private final com.marzouki.shop.invoice.InvoiceService invoices;

    @PostMapping("/orders")
    @Operation(summary = "Place an order",
            description = "Needs an Idempotency-Key header (8 to 64 letters, digits, - or _). Sending the same key and body again "
                    + "returns the same order with the header Idempotent-Replayed: true instead of ordering twice. "
                    + "Stock is reserved atomically; the call fails with 409 if an item is unavailable or prices changed.")
    public ResponseEntity<OrderResponse> place(@AuthenticationPrincipal Jwt token,
                                               @Parameter(description = "Unique per order attempt, e.g. a UUID") @RequestHeader(value = "Idempotency-Key", required = false) String key,
                                               @Valid @RequestBody OrderRequest request) {
        if (key == null || !KEY.matcher(key).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Send an Idempotency-Key header of 8 to 64 letters, digits, - or _");
        }
        Long userId = userId(token);
        OrderApiService.Placed placed;
        try {
            placed = orders.place(userId, key, request);
        } catch (DataIntegrityViolationException e) {
            placed = orders.replay(userId, key, request);
        }
        OrderResponse body = orders.mineById(userId, placed.orderId());
        if (placed.replayed()) {
            return ResponseEntity.ok().header("Idempotent-Replayed", "true").body(body);
        }
        return ResponseEntity.created(URI.create("/api/orders/" + body.id())).body(body);
    }

    @GetMapping("/orders")
    @Operation(summary = "List your orders", description = "status is an order status; sort is newest, oldest, total-desc or total-asc. Page size is one of 10, 25, 50 or 100; anything else gives 25.")
    public PageResponse<OrderSummary> mine(@AuthenticationPrincipal Jwt token, @RequestParam(defaultValue = "") String status,
                                           @RequestParam(defaultValue = "") String sort, @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "25") int size) {
        return orders.mine(userId(token), query("", status, sort, page, size));
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Get one of your orders")
    public OrderResponse mineById(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        return orders.mineById(userId(token), id);
    }

    @PostMapping("/orders/{id}/cancel")
    @Operation(summary = "Cancel your order", description = "Only while it is still Placed. Stock and coupon use are returned.")
    public OrderResponse cancel(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        return orders.cancel(userId(token), id);
    }

    @GetMapping(value = "/orders/{id}/invoice", produces = "application/pdf")
    @Operation(summary = "Download the invoice of one of your orders as a PDF")
    public ResponseEntity<byte[]> myInvoice(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        orders.mineById(userId(token), id);
        return com.marzouki.shop.invoice.InvoiceController.pdf(invoices.document(id));
    }

    @GetMapping(value = "/manage/orders/{id}/invoice", produces = "application/pdf")
    @Operation(summary = "Download the invoice of any order as a PDF (managers)")
    public ResponseEntity<byte[]> invoice(@PathVariable Long id) {
        orders.byId(id);
        return com.marzouki.shop.invoice.InvoiceController.pdf(invoices.document(id));
    }

    @GetMapping("/manage/orders")
    @Operation(summary = "List all orders (managers)", description = "q matches the order number, customer name or email, and city.")
    public PageResponse<OrderSummary> all(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String status,
                                          @RequestParam(defaultValue = "") String sort, @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "25") int size) {
        return orders.all(query(q, status, sort, page, size));
    }

    @GetMapping("/manage/orders/{id}")
    @Operation(summary = "Get any order (managers)")
    public OrderResponse byId(@PathVariable Long id) {
        return orders.byId(id);
    }

    @PostMapping("/manage/orders/{id}/status")
    @Operation(summary = "Move an order to its next status (managers)",
            description = "PLACED to PROCESSING or CANCELLED, PROCESSING to SHIPPED or CANCELLED, SHIPPED to DELIVERED.")
    public OrderResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return orders.changeStatus(id, request.status());
    }

    private static ListQuery query(String q, String status, String sort, int page, int size) {
        ListQuery query = new ListQuery();
        query.setQ(q);
        query.setStatus(status);
        query.setSort(sort);
        query.setPage(page);
        query.setSize(size);
        return query;
    }

    private static Long userId(Jwt token) {
        return Long.valueOf(token.getSubject());
    }
}