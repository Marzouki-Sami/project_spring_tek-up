package com.marzouki.shop.geo;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Makes the map settings available to every page as {@code geo}. */
@ControllerAdvice
@RequiredArgsConstructor
class GeoAdvice {

    private final GeoView view;

    @ModelAttribute("geo")
    GeoView geo() {
        return view;
    }
}