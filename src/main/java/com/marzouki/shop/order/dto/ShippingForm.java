package com.marzouki.shop.order.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Where an order goes. Either a saved address is chosen ({@code addressId}), or the fields below are
 * filled in by hand; the checks apply only in the second case (the {@link Manual} group).
 */
@Getter
@Setter
public class ShippingForm {

    /** Validation group for an address typed in rather than picked from the saved ones. */
    public interface Manual {
    }

    private Long addressId;

    private boolean saveAddress;

    @Size(max = 40, message = "Use at most 40 characters")
    private String addressLabel;

    @NotBlank(groups = Manual.class, message = "Enter the recipient's name")
    @Size(max = 160, message = "Use at most 160 characters")
    private String fullName;

    @Size(max = 30, message = "Use at most 30 characters")
    @Pattern(regexp = "[0-9+()\\-. ]*", message = "Use digits, spaces and + ( ) - . only")
    private String phone;

    @NotBlank(groups = Manual.class, message = "Enter the street address")
    @Size(max = 200, message = "Use at most 200 characters")
    private String address;

    @NotBlank(groups = Manual.class, message = "Enter the city")
    @Size(max = 100, message = "Use at most 100 characters")
    private String city;

    @NotBlank(groups = Manual.class, message = "Enter the postal code")
    @Size(max = 20, message = "Use at most 20 characters")
    private String postalCode;

    @NotBlank(groups = Manual.class, message = "Enter the country")
    @Size(max = 80, message = "Use at most 80 characters")
    private String country;

    @DecimalMin(value = "-90", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90", message = "Latitude must be between -90 and 90")
    private BigDecimal latitude;

    @DecimalMin(value = "-180", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180", message = "Longitude must be between -180 and 180")
    private BigDecimal longitude;

    /** The total the customer saw on the checkout page; the order is refused if it no longer matches. */
    private BigDecimal expectedTotal;
}