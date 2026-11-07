package com.marzouki.shop.order;

import com.marzouki.shop.catalog.ProductVariant;
import com.marzouki.shop.catalog.ProductVariantRepository;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.CurrentActor;
import com.marzouki.shop.common.NotFoundException;
import com.marzouki.shop.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manual stock changes. Each one is recorded with a reason, a note and who made it. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final ProductVariantRepository variants;
    private final StockMovementRepository movements;
    private final UserRepository users;
    private final ApplicationEventPublisher events;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Transactional
    public void adjust(Long variantId, int delta, StockMovement.Reason reason, String note) {
        if (delta == 0) {
            throw new ConflictException("Enter a change other than zero");
        }
        ProductVariant variant = variants.findById(variantId).orElseThrow(() -> new NotFoundException("Variant", variantId));
        int before = variant.getStock();
        if (variants.adjust(variantId, delta) == 0) {
            throw new ConflictException("Only " + before + " in stock, so that many cannot be removed");
        }
        entityManager.refresh(variant);
        StockMovement movement = new StockMovement(variant.getProduct().getId(), variantId, delta, reason, null);
        String trimmed = note == null ? "" : note.trim();
        movement.setNote(trimmed.isEmpty() ? null : trimmed);
        Long actorId = CurrentActor.id();
        movement.setActor(actorId == null ? null : users.getReferenceById(actorId));
        movements.save(movement);
        if (before == 0 && before + delta > 0) {
            events.publishEvent(new OrderEvents.BackInStock(variantId));
        }
    }

    public Page<StockMovement> history(Long variantId, int page) {
        return movements.findByVariantIdOrderByCreatedAtDescIdDesc(variantId, PageRequest.of(Math.max(page, 0), 20));
    }
}