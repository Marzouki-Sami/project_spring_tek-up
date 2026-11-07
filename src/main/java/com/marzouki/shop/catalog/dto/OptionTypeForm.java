package com.marzouki.shop.catalog.dto;

import com.marzouki.shop.catalog.OptionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OptionTypeForm {

    @NotBlank(message = "Enter a name")
    @Size(max = 60, message = "Use at most 60 characters")
    private String name;

    private OptionType.Display display = OptionType.Display.TEXT;
}