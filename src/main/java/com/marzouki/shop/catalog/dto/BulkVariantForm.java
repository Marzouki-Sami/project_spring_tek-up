package com.marzouki.shop.catalog.dto;

import com.marzouki.shop.order.StockMovement;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** One action applied to several variants of a product at once. */
@Getter
@Setter
public class BulkVariantForm {

    public enum Action { SET_PRICE, CLEAR_PRICE, ADJUST_STOCK, ACTIVATE, DEACTIVATE, DELETE }

    public enum Scope { SELECTED, FILTERED }

    @Size(max = 500, message = "Select at most 500 variants at a time")
    private List<Long> ids = new ArrayList<>();

    private Scope scope = Scope.SELECTED;

    private Action action;

    @DecimalMin(value = "0.00", message = "The price cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Use at most 2 decimals")
    private BigDecimal price;

    @Min(value = -100000, message = "That change is too large")
    @Max(value = 100000, message = "That change is too large")
    private Integer delta;

    private StockMovement.Reason reason = StockMovement.Reason.SUPPLIER_DELIVERY;

    @Size(max = 200, message = "Use at most 200 characters")
    private String note;
}
