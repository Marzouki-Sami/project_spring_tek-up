package com.marzouki.shop.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductForm {

    @NotBlank(message = "Enter a SKU")
    @Size(max = 40, message = "Use at most 40 characters")
    @Pattern(regexp = "[A-Za-z0-9._-]*", message = "Use letters, digits, dots, dashes and underscores only")
    private String sku;

    @NotBlank(message = "Enter a name")
    @Size(max = 150, message = "Use at most 150 characters")
    private String name;

    @Size(max = 2000, message = "Use at most 2000 characters")
    private String description;

    @NotNull(message = "Enter a price")
    @DecimalMin(value = "0.00", message = "The price cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Use at most 2 decimals")
    private BigDecimal price;

    @Min(value = 0, message = "Stock cannot be negative")
    private int stock;

    private boolean active = true;

    @NotNull(message = "Choose a subcategory")
    private Long subcategoryId;

    private Long supplierId;
}