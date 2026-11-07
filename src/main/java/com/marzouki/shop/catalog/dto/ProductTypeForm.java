package com.marzouki.shop.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductTypeForm {

    @NotBlank(message = "Enter a name")
    @Size(max = 80, message = "Use at most 80 characters")
    private String name;

    private List<Long> optionTypeIds = new ArrayList<>();
}