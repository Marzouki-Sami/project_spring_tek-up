package com.marzouki.shop.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class CouponForm {

    @NotBlank(message = "Enter a code")
    @Size(max = 40, message = "Use at most 40 characters")
    @Pattern(regexp = "[A-Za-z0-9_-]*", message = "Use letters, digits, dashes and underscores only")
    private String code;

    @Min(value = 1, message = "Enter a discount between 1 and 100")
    @Max(value = 100, message = "Enter a discount between 1 and 100")
    private int percentOff = 10;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiresOn;

    @Min(value = 1, message = "The limit must be at least 1")
    private Integer maxUses;
}