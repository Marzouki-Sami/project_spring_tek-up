package com.marzouki.shop.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VariantForm {

    @NotBlank(message = "Enter a SKU")
    @Size(max = 60, message = "Use at most 60 characters")
    @Pattern(regexp = "[A-Za-z0-9._-]*", message = "Use letters, digits, dots, dashes and underscores only")
    private String sku;

    @DecimalMin(value = "0.00", message = "The price cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Use at most 2 decimals")
    private BigDecimal price;

    private boolean active = true;
}