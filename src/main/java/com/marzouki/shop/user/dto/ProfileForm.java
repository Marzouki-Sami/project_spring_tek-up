package com.marzouki.shop.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileForm {

    @NotBlank(message = "Enter your first name")
    @Size(max = 80, message = "Use at most 80 characters")
    private String firstName;

    @NotBlank(message = "Enter your last name")
    @Size(max = 80, message = "Use at most 80 characters")
    private String lastName;

    @Size(max = 30, message = "Use at most 30 characters")
    @Pattern(regexp = "[0-9+()\\-. ]*", message = "Use digits, spaces and + ( ) - . only")
    private String phone;
}