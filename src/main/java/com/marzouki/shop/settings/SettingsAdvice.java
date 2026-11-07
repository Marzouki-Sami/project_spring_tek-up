package com.marzouki.shop.settings;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Makes the shop name available to every page. */
@ControllerAdvice
@RequiredArgsConstructor
class SettingsAdvice {

    private final SettingsService settings;

    @ModelAttribute("shopName")
    String shopName() {
        return settings.shopName();
    }
}