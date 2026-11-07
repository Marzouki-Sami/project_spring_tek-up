package com.marzouki.shop.order.web;

import com.marzouki.shop.address.Address;
import com.marzouki.shop.address.AddressService;
import com.marzouki.shop.address.dto.AddressForm;
import com.marzouki.shop.cart.Cart;
import com.marzouki.shop.cart.CartService;
import com.marzouki.shop.cart.CartStore;
import com.marzouki.shop.cart.CartView;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.mail.MailProperties;
import com.marzouki.shop.order.Order;
import com.marzouki.shop.order.OrderService;
import com.marzouki.shop.order.dto.ShippingForm;
import com.marzouki.shop.security.AppUserDetails;
import com.marzouki.shop.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Checkout needs an account: a guest is sent to sign in or register and returns here afterwards. */
@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final Cart cart;
    private final CartService carts;
    private final OrderService orders;
    private final UserService users;
    private final MailProperties mail;
    private final CartStore cartStore;
    private final AddressService addresses;
    private final SmartValidator validator;

    @GetMapping
    public String form(@AuthenticationPrincipal AppUserDetails me, Model model, RedirectAttributes flash) {
        if (mustVerify(me)) {
            flash.addFlashAttribute("error", "Please confirm your email address before checking out");
            return "redirect:/account";
        }
        CartView view = carts.view(cart);
        if (view.isEmpty()) {
            return "redirect:/cart";
        }
        java.util.List<Address> saved = addresses.forUser(me.getId());
        ShippingForm form = new ShippingForm();
        form.setFullName(me.getFullName());
        form.setAddressLabel("Home");
        form.setSaveAddress(saved.isEmpty());
        saved.stream().filter(Address::isDefaultAddress).findFirst().or(() -> saved.stream().findFirst())
                .ifPresent(a -> form.setAddressId(a.getId()));
        form.setExpectedTotal(view.total());
        return show(model, form, view, saved);
    }

    @PostMapping
    public String place(@AuthenticationPrincipal AppUserDetails me,
                        @ModelAttribute("form") ShippingForm form, BindingResult result, Model model,
                        RedirectAttributes flash) {
        if (mustVerify(me)) {
            flash.addFlashAttribute("error", "Please confirm your email address before checking out");
            return "redirect:/account";
        }
        Address chosen = null;
        if (form.getAddressId() != null) {
            chosen = addresses.get(form.getAddressId(), me.getId());
            form.setFullName(chosen.getFullName());
            form.setPhone(chosen.getPhone());
            form.setAddress(chosen.getStreet());
            form.setPostalCode(chosen.getPostalCode());
            form.setCity(chosen.getCity());
            form.setCountry(chosen.getCountry());
            form.setLatitude(chosen.getLatitude());
            form.setLongitude(chosen.getLongitude());
        } else {
            validator.validate(form, result, ShippingForm.Manual.class);
        }
        validator.validate(form, result);
        if (!result.hasErrors()) {
            try {
                Order order = orders.place(me.getId(), form, cart);
                cart.clear();
                cartStore.clear(me.getId());
                if (chosen == null && form.isSaveAddress()) {
                    saveAddress(me, form, flash);
                }
                return "redirect:/account/orders/" + order.getId() + "?placed";
            } catch (ConflictException e) {
                result.reject("checkout", e.getMessage());
            }
        }
        CartView view = carts.view(cart);
        if (view.isEmpty()) {
            return "redirect:/cart";
        }
        form.setExpectedTotal(view.total());
        return show(model, form, view, addresses.forUser(me.getId()));
    }

    private void saveAddress(AppUserDetails me, ShippingForm form, RedirectAttributes flash) {
        AddressForm copy = new AddressForm();
        copy.setLabel(form.getAddressLabel() == null || form.getAddressLabel().isBlank() ? "Home" : form.getAddressLabel());
        copy.setFullName(form.getFullName());
        copy.setPhone(form.getPhone());
        copy.setAddress(form.getAddress());
        copy.setPostalCode(form.getPostalCode());
        copy.setCity(form.getCity());
        copy.setCountry(form.getCountry());
        copy.setLatitude(form.getLatitude());
        copy.setLongitude(form.getLongitude());
        try {
            addresses.create(me.getId(), copy);
            flash.addFlashAttribute("info", "Your address was saved to your account");
        } catch (ConflictException e) {
            flash.addFlashAttribute("warning", "Your order is placed, but the address was not saved: " + e.getMessage());
        }
    }

    private String show(Model model, ShippingForm form, CartView view, java.util.List<Address> saved) {
        model.addAttribute("form", form);
        model.addAttribute("cart", view);
        model.addAttribute("addresses", saved);
        return "checkout/checkout";
    }

    /** With real email delivery on, an unconfirmed address cannot place orders (we could not reach the buyer). */
    private boolean mustVerify(AppUserDetails me) {
        return mail.enabled() && !users.get(me.getId()).isEmailVerified();
    }
}