package com.marzouki.shop.order.web;

import com.marzouki.shop.cart.Cart;
import com.marzouki.shop.cart.CartService;
import com.marzouki.shop.cart.CartView;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.order.Order;
import com.marzouki.shop.order.OrderService;
import com.marzouki.shop.order.dto.ShippingForm;
import com.marzouki.shop.security.AppUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Checkout needs an account: a guest is sent to sign in or register and returns here afterwards. */
@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final Cart cart;
    private final CartService carts;
    private final OrderService orders;
    private final com.marzouki.shop.user.UserService users;
    private final com.marzouki.shop.mail.MailProperties mail;

    @GetMapping
    public String form(@AuthenticationPrincipal AppUserDetails me, Model model,
                       org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        if (mustVerify(me)) {
            flash.addFlashAttribute("error", "Please confirm your email address before checking out");
            return "redirect:/account";
        }
        CartView view = carts.view(cart);
        if (view.isEmpty()) {
            return "redirect:/cart";
        }
        ShippingForm form = new ShippingForm();
        form.setFullName(me.getFullName());
        form.setExpectedTotal(view.total());
        model.addAttribute("form", form);
        model.addAttribute("cart", view);
        return "checkout/checkout";
    }

    @PostMapping
    public String place(@AuthenticationPrincipal AppUserDetails me,
                        @Valid @ModelAttribute("form") ShippingForm form, BindingResult result, Model model,
                        org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        if (mustVerify(me)) {
            flash.addFlashAttribute("error", "Please confirm your email address before checking out");
            return "redirect:/account";
        }
        if (!result.hasErrors()) {
            try {
                Order order = orders.place(me.getId(), form, cart);
                cart.clear();
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
        model.addAttribute("cart", view);
        return "checkout/checkout";
    }

    /** With real email delivery on, an unconfirmed address cannot place orders (we could not reach the buyer). */
    private boolean mustVerify(AppUserDetails me) {
        return mail.enabled() && !users.get(me.getId()).isEmailVerified();
    }
}
