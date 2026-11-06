package com.marzouki.shop.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShippingForm {

    @NotBlank(message = "Enter the recipient's name")
    @Size(max = 160, message = "Use at most 160 characters")
    private String fullName;

    @NotBlank(message = "Enter the street address")
    @Size(max = 200, message = "Use at most 200 characters")
    private String address;

    @NotBlank(message = "Enter the city")
    @Size(max = 100, message = "Use at most 100 characters")
    private String city;

    @NotBlank(message = "Enter the postal code")
    @Size(max = 20, message = "Use at most 20 characters")
    private String postalCode;

    @NotBlank(message = "Enter the country")
    @Size(max = 80, message = "Use at most 80 characters")
    private String country;
}