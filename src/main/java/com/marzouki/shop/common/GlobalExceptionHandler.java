package com.marzouki.shop.common;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Turns domain exceptions into friendly error pages. */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(NotFoundException ex, Model model) {
        model.addAttribute("title", "Not found");
        model.addAttribute("message", ex.getMessage());
        return "error/message";
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String conflict(ConflictException ex, Model model) {
        model.addAttribute("title", "Conflict");
        model.addAttribute("message", ex.getMessage());
        return "error/message";
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public String tooLarge(Model model) {
        model.addAttribute("title", "Upload too large");
        model.addAttribute("message", "Each image can be at most 5 MB, and an upload at most 30 MB in total.");
        return "error/message";
    }
}