package com.marzouki.shop.user.dto;

import com.marzouki.shop.user.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordForm {

    private String token;

    @NotBlank(message = "Choose a password")
    @StrongPassword
    private String newPassword;

    @NotBlank(message = "Repeat your password")
    private String confirmPassword;
}