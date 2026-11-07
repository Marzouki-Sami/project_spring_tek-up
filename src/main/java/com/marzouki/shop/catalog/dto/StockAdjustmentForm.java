package com.marzouki.shop.catalog.dto;

import com.marzouki.shop.order.StockMovement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockAdjustmentForm {

    @Min(value = -100000, message = "That change is too large")
    @Max(value = 100000, message = "That change is too large")
    private int delta;

    @NotNull(message = "Choose a reason")
    private StockMovement.Reason reason = StockMovement.Reason.SUPPLIER_DELIVERY;

    @Size(max = 200, message = "Use at most 200 characters")
    private String note;
}