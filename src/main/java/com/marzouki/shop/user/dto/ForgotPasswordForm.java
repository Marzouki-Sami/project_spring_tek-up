package com.marzouki.shop.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordForm {

    @NotBlank(message = "Enter your email address")
    @Email(message = "Enter a valid email address")
    private String email;
}