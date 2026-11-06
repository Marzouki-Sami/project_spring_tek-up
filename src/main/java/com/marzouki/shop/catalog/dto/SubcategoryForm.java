package com.marzouki.shop.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubcategoryForm {

    @NotBlank(message = "Enter a title")
    @Size(max = 80, message = "Use at most 80 characters")
    private String title;

    @Size(max = 500, message = "Use at most 500 characters")
    private String description;

    @NotNull(message = "Choose a category")
    private Long categoryId;
}