package com.marzouki.shop.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return true; // @NotBlank reports a missing value
        }
        if (password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return false;
        }
        boolean lower = false;
        boolean upper = false;
        boolean digit = false;
        boolean symbol = false;
        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else if (!Character.isWhitespace(c)) {
                symbol = true;
            }
        }
        return lower && upper && digit && symbol;
    }
}