package com.marzouki.shop.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A password with 8 to 72 characters, containing a lowercase letter, an uppercase letter, a digit
 * and a symbol. The upper limit matches what BCrypt can use.
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "Use 8 to 72 characters with an uppercase letter, a lowercase letter, a digit and a symbol";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}