package com.marzouki.shop.media;

import com.marzouki.shop.common.ConflictException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Back-office page for a product's pictures. */
@Controller
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageService images;

    @GetMapping("/manage/products/{productId}/images")
    public String page(@PathVariable Long productId, Model model) {
        model.addAttribute("product", images.productWithImages(productId));
        model.addAttribute("max", ProductImageService.MAX_IMAGES);
        return "image/images";
    }

    @PostMapping("/manage/products/{productId}/images")
    public String upload(@PathVariable Long productId, @RequestParam("files") MultipartFile[] files,
                         RedirectAttributes flash) throws IOException {
        int added = 0;
        try {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    images.add(productId, file.getBytes(), null);
                    added++;
                }
            }
            if (added == 0) {
                flash.addFlashAttribute("error", "Choose at least one image");
            } else {
                flash.addFlashAttribute("success", added == 1 ? "Image added" : added + " images added");
            }
        } catch (ConflictException e) {
            flash.addFlashAttribute("error", (added > 0 ? added + " added, then: " : "") + e.getMessage());
        }
        return back(productId);
    }

    @PostMapping("/manage/products/{productId}/images/{imageId}/move")
    public String move(@PathVariable Long productId, @PathVariable Long imageId, @RequestParam int direction) {
        images.move(productId, imageId, direction);
        return back(productId);
    }

    @PostMapping("/manage/products/{productId}/images/{imageId}/preview")
    public String preview(@PathVariable Long productId, @PathVariable Long imageId, RedirectAttributes flash) {
        images.makePreview(productId, imageId);
        flash.addFlashAttribute("success", "This image is now the preview");
        return back(productId);
    }

    @PostMapping("/manage/products/{productId}/images/{imageId}/alt")
    public String alt(@PathVariable Long productId, @PathVariable Long imageId,
                      @RequestParam(defaultValue = "") String altText, RedirectAttributes flash) {
        images.updateAlt(productId, imageId, altText);
        flash.addFlashAttribute("success", "Description saved");
        return back(productId);
    }

    @PostMapping("/manage/products/{productId}/images/{imageId}/delete")
    public String delete(@PathVariable Long productId, @PathVariable Long imageId, RedirectAttributes flash) {
        images.delete(productId, imageId);
        flash.addFlashAttribute("success", "Image removed");
        return back(productId);
    }

    private static String back(Long productId) {
        return "redirect:/manage/products/" + productId + "/images";
    }
}