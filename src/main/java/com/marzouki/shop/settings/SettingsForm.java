package com.marzouki.shop.settings;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingsForm {

    @NotBlank(message = "Enter the shop name")
    @Size(max = 80, message = "Use at most 80 characters")
    private String shopName;

    @NotBlank(message = "Choose a colour")
    @Pattern(regexp = "#[0-9a-fA-F]{6}", message = "Use a colour like #4f46e5")
    private String accentColor;

    @NotBlank(message = "Enter the footer text")
    @Size(max = 300, message = "Use at most 300 characters")
    private String footerText;

    @Email(message = "Enter a valid email address")
    @Size(max = 254, message = "Use at most 254 characters")
    private String supportEmail;

    @Size(max = 300, message = "Use at most 300 characters")
    private String companyAddress;

    @Size(max = 40, message = "Use at most 40 characters")
    private String vatNumber;

    @NotNull(message = "Enter the VAT rate")
    @DecimalMin(value = "0", message = "The VAT rate cannot be negative")
    @DecimalMax(value = "40", message = "Use a rate of 40% or less")
    private BigDecimal vatPercent;

    @Size(max = 300, message = "Use at most 300 characters")
    private String invoiceNote;
}