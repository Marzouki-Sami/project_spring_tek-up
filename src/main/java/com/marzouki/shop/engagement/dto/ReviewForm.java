package com.marzouki.shop.engagement.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewForm {

    @Min(value = 1, message = "Choose a rating from 1 to 5")
    @Max(value = 5, message = "Choose a rating from 1 to 5")
    private int rating;

    @Size(max = 1000, message = "Use at most 1000 characters")
    private String comment;
}