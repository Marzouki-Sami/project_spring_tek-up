package com.marzouki.shop.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.marzouki.shop.order.Order;
import com.marzouki.shop.order.OrderItem;
import com.marzouki.shop.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Request and response bodies of the order endpoints. */
public final class OrderDtos {

    private OrderDtos() {
    }

    public record Line(
            @NotNull Long variantId,
            @Min(1) @Max(20) int quantity) {
    }

    @Schema(description = "Where to deliver. Give a saved addressId, or fullName, address, city, postalCode and country.")
    public record Shipping(
            Long addressId,
            @Size(max = 160) String fullName,
            @Size(max = 30) @Pattern(regexp = "[0-9+()\\-. ]*") String phone,
            @Size(max = 200) String address,
            @Size(max = 100) String city,
            @Size(max = 20) String postalCode,
            @Size(max = 80) String country,
            @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
            @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude) {
    }

    public record OrderRequest(
            @NotEmpty @Size(max = 50) List<@Valid @NotNull Line> items,
            @Size(max = 40) String couponCode,
            @NotNull @Valid Shipping shipping,
            @Schema(description = "The total you showed the customer. The order is refused if prices changed.")
            BigDecimal expectedTotal) {
    }

    public record StatusRequest(@NotNull OrderStatus status) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OrderSummary(Long id, OrderStatus status, Instant createdAt, BigDecimal total, String customerName,
                               String customerEmail) {
        public static OrderSummary of(Order order, boolean withCustomer) {
            return new OrderSummary(order.getId(), order.getStatus(), order.getCreatedAt(), order.getTotal(),
                    withCustomer ? order.getUser().getFullName() : null,
                    withCustomer ? order.getUser().getEmail() : null);
        }
    }

    public record ItemResponse(Long variantId, String sku, String name, String variantLabel, BigDecimal unitPrice,
                               int quantity, BigDecimal lineTotal) {
        static ItemResponse of(OrderItem item) {
            return new ItemResponse(item.getVariant() == null ? null : item.getVariant().getId(), item.getSku(),
                    item.getName(), item.getVariantLabel(), item.getUnitPrice(), item.getQuantity(),
                    item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OrderResponse(Long id, OrderStatus status, Instant createdAt, String couponCode, BigDecimal subtotal,
                                BigDecimal discount, BigDecimal total, Shipping shipping, List<ItemResponse> items,
                                String customerName, String customerEmail) {
        public static OrderResponse of(Order order, boolean withCustomer) {
            Shipping shipping = new Shipping(null, order.getFullName(), order.getPhone(), order.getAddress(),
                    order.getCity(), order.getPostalCode(), order.getCountry(), order.getLatitude(), order.getLongitude());
            return new OrderResponse(order.getId(), order.getStatus(), order.getCreatedAt(), order.getCouponCode(),
                    order.getSubtotal(), order.getDiscount(), order.getTotal(), shipping,
                    order.getItems().stream().map(ItemResponse::of).toList(),
                    withCustomer ? order.getUser().getFullName() : null,
                    withCustomer ? order.getUser().getEmail() : null);
        }
    }
}