package com.marzouki.shop.user.dto;

import com.marzouki.shop.user.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordChangeForm {

    @NotBlank(message = "Enter your current password")
    private String currentPassword;

    @NotBlank(message = "Choose a new password")
    @StrongPassword
    private String newPassword;

    @NotBlank(message = "Repeat the new password")
    private String confirmPassword;
}