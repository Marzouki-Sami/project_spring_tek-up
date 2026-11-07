package com.marzouki.shop.engagement.web;

import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.engagement.EngagementService;
import com.marzouki.shop.engagement.dto.ReviewForm;
import com.marzouki.shop.security.AppUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Reviews and the wishlist. Everything here needs a signed-in user. */
@Controller
@RequiredArgsConstructor
public class EngagementController {

    private final EngagementService engagement;

    @PostMapping("/reviews")
    public String review(@AuthenticationPrincipal AppUserDetails me, @RequestParam Long productId,
                         @Valid @ModelAttribute ReviewForm form, BindingResult result, RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("error", result.getFieldErrors().get(0).getDefaultMessage());
        } else {
            try {
                engagement.submit(me.getId(), productId, form);
                flash.addFlashAttribute("success", "Thanks for your review");
            } catch (ConflictException e) {
                flash.addFlashAttribute("error", e.getMessage());
            }
        }
        return "redirect:/products/" + productId + "#reviews";
    }

    @PostMapping("/reviews/{id}/delete")
    public String deleteReview(@AuthenticationPrincipal AppUserDetails me, @PathVariable Long id,
                               RedirectAttributes flash) {
        Long productId = engagement.delete(id, me.getId(), me.getRole());
        flash.addFlashAttribute("success", "Review removed");
        return "redirect:/products/" + productId + "#reviews";
    }

    @PostMapping("/wishlist/toggle")
    public String toggle(@AuthenticationPrincipal AppUserDetails me, @RequestParam Long productId,
                         @RequestParam(defaultValue = "product") String from, RedirectAttributes flash) {
        boolean added = engagement.toggleWish(me.getId(), productId);
        flash.addFlashAttribute(added ? "success" : "info", added ? "Saved to your wishlist" : "Removed from your wishlist");
        return "wishlist".equals(from) ? "redirect:/account/wishlist" : "redirect:/products/" + productId;
    }

    @GetMapping("/account/wishlist")
    public String wishlist(@AuthenticationPrincipal AppUserDetails me, Model model) {
        model.addAttribute("items", engagement.wishlistOf(me.getId()));
        return "wishlist/wishlist";
    }
}