package com.marzouki.shop.catalog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierForm {

    @NotBlank(message = "Enter the company name")
    @Size(max = 120, message = "Use at most 120 characters")
    private String company;

    @Size(max = 120, message = "Use at most 120 characters")
    private String contactName;

    @Email(message = "Enter a valid email address")
    @Size(max = 254, message = "Use at most 254 characters")
    private String email;

    @Size(max = 30, message = "Use at most 30 characters")
    private String phone;

    @Size(max = 255, message = "Use at most 255 characters")
    private String address;

    @Size(max = 60, message = "Use at most 60 characters")
    private String registrationNumber;

    private boolean active = true;
}