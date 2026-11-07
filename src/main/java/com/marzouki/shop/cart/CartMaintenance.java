package com.marzouki.shop.cart;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
class CartMaintenance {

    private final CartStore store;

    @Scheduled(cron = "0 30 3 * * *")
    void removeStaleCarts() {
        int removed = store.deleteStale();
        if (removed > 0) {
            log.info("Removed {} abandoned baskets", removed);
        }
    }
}