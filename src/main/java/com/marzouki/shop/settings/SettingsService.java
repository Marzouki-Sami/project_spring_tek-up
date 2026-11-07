package com.marzouki.shop.settings;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads and changes the shop settings. The current values are kept in memory and refreshed on every save. */
@Service
@RequiredArgsConstructor
public class SettingsService {

    private final ShopSettingsRepository repository;
    private final AtomicReference<ShopSettings> cache = new AtomicReference<>();

    /** A snapshot of the settings. Do not modify it. */
    public ShopSettings get() {
        ShopSettings cached = cache.get();
        if (cached == null) {
            cached = load();
            cache.set(cached);
        }
        return cached;
    }

    public String shopName() {
        return get().getShopName();
    }

    public SettingsForm form() {
        ShopSettings s = get();
        SettingsForm form = new SettingsForm();
        form.setShopName(s.getShopName());
        form.setAccentColor(s.getAccentColor());
        form.setFooterText(s.getFooterText());
        form.setSupportEmail(s.getSupportEmail());
        form.setCompanyAddress(s.getCompanyAddress());
        form.setVatNumber(s.getVatNumber());
        form.setVatPercent(s.getVatPercent());
        form.setInvoiceNote(s.getInvoiceNote());
        return form;
    }

    @Transactional
    public void save(SettingsForm form) {
        ShopSettings s = repository.findById(ShopSettings.ID).orElseGet(ShopSettings::new);
        s.setShopName(form.getShopName().trim());
        s.setAccentColor(form.getAccentColor().toLowerCase());
        s.setFooterText(form.getFooterText().trim());
        s.setSupportEmail(blankToNull(form.getSupportEmail()));
        s.setCompanyAddress(blankToNull(form.getCompanyAddress()));
        s.setVatNumber(blankToNull(form.getVatNumber()));
        s.setVatPercent(form.getVatPercent());
        s.setInvoiceNote(blankToNull(form.getInvoiceNote()));
        s.setUpdatedAt(Instant.now());
        repository.saveAndFlush(s);
        cache.set(null);
    }

    @Transactional(readOnly = true)
    ShopSettings load() {
        ShopSettings s = repository.findById(ShopSettings.ID).orElseGet(() -> {
            ShopSettings fallback = new ShopSettings();
            fallback.setShopName("Shop");
            fallback.setAccentColor("#4f46e5");
            fallback.setFooterText("You received this email because of activity on your Shop account.");
            fallback.setVatPercent(new BigDecimal("20.0"));
            return fallback;
        });
        return copy(s);
    }

    private static ShopSettings copy(ShopSettings s) {
        ShopSettings c = new ShopSettings();
        c.setShopName(s.getShopName());
        c.setAccentColor(s.getAccentColor());
        c.setFooterText(s.getFooterText());
        c.setSupportEmail(s.getSupportEmail());
        c.setCompanyAddress(s.getCompanyAddress());
        c.setVatNumber(s.getVatNumber());
        c.setVatPercent(s.getVatPercent());
        c.setInvoiceNote(s.getInvoiceNote());
        c.setUpdatedAt(s.getUpdatedAt());
        return c;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}