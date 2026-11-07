package com.marzouki.shop.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OptionValueForm {

    @NotBlank(message = "Enter a value")
    @Size(max = 60, message = "Use at most 60 characters")
    private String label;

    @Pattern(regexp = "(#[0-9a-fA-F]{6})?", message = "Use a colour like #1a2b3c")
    private String colorHex;
}