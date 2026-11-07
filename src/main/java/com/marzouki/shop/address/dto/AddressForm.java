package com.marzouki.shop.address.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressForm {

    @NotBlank(message = "Give this address a name, such as Home")
    @Size(max = 40, message = "Use at most 40 characters")
    private String label;

    @NotBlank(message = "Enter the recipient's name")
    @Size(max = 160, message = "Use at most 160 characters")
    private String fullName;

    @Size(max = 30, message = "Use at most 30 characters")
    @Pattern(regexp = "[0-9+()\\-. ]*", message = "Use digits, spaces and + ( ) - . only")
    private String phone;

    @NotBlank(message = "Enter the street address")
    @Size(max = 200, message = "Use at most 200 characters")
    private String address;

    @NotBlank(message = "Enter the postal code")
    @Size(max = 20, message = "Use at most 20 characters")
    private String postalCode;

    @NotBlank(message = "Enter the city")
    @Size(max = 100, message = "Use at most 100 characters")
    private String city;

    @NotBlank(message = "Enter the country")
    @Size(max = 80, message = "Use at most 80 characters")
    private String country;

    @DecimalMin(value = "-90", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90", message = "Latitude must be between -90 and 90")
    private BigDecimal latitude;

    @DecimalMin(value = "-180", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180", message = "Longitude must be between -180 and 180")
    private BigDecimal longitude;

    private boolean defaultAddress;
}