package com.marzouki.shop.mail;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailTemplateForm {

    @NotBlank(message = "Enter a subject")
    @Size(max = 200, message = "Use at most 200 characters")
    private String subject;

    @NotBlank(message = "Enter a heading")
    @Size(max = 120, message = "Use at most 120 characters")
    private String heading;

    @NotBlank(message = "Enter the message")
    @Size(max = 2000, message = "Use at most 2000 characters")
    private String body;

    @Size(max = 40, message = "Use at most 40 characters")
    private String buttonLabel;
}