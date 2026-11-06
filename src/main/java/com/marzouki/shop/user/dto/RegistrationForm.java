package com.marzouki.shop.user.dto;

import com.marzouki.shop.user.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationForm {

    @NotBlank(message = "Enter your first name")
    @Size(max = 80, message = "Use at most 80 characters")
    private String firstName;

    @NotBlank(message = "Enter your last name")
    @Size(max = 80, message = "Use at most 80 characters")
    private String lastName;

    @NotBlank(message = "Enter your email address")
    @Email(message = "Enter a valid email address")
    @Size(max = 254, message = "Use at most 254 characters")
    private String email;

    @NotBlank(message = "Choose a password")
    @StrongPassword
    private String password;

    @NotBlank(message = "Repeat your password")
    private String confirmPassword;
}