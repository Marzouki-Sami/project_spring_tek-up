package com.marzouki.shop.engagement;

import com.marzouki.shop.catalog.ProductRepository;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.NotFoundException;
import com.marzouki.shop.engagement.dto.ReviewForm;
import com.marzouki.shop.order.OrderRepository;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Product reviews (verified buyers only) and the personal wishlist. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EngagementService {

    private final ReviewRepository reviews;
    private final WishlistRepository wishlist;
    private final ProductRepository products;
    private final UserRepository users;
    private final OrderRepository orders;

    public boolean hasBought(Long userId, Long productId) {
        return orders.hasPurchased(userId, productId);
    }

    public List<Review> reviewsFor(Long productId) {
        return reviews.findByProductIdOrderByCreatedAtDescIdDesc(productId);
    }

    public Review reviewBy(Long userId, Long productId) {
        return reviews.findByProductIdAndUserId(productId, userId).orElse(null);
    }

    public Double averageRating(Long productId) {
        return reviews.averageRating(productId);
    }

    /** Adds or replaces the buyer's review. Only people who bought the product (and kept the order) may review. */
    @Transactional
    public Review submit(Long userId, Long productId, ReviewForm form) {
        if (!hasBought(userId, productId)) {
            throw new ConflictException("Only customers who bought this product can review it");
        }
        Review review = reviews.findByProductIdAndUserId(productId, userId).orElseGet(() -> {
            Review created = new Review();
            created.setProduct(products.getReferenceById(productId));
            created.setUser(users.getReferenceById(userId));
            return created;
        });
        review.setRating(form.getRating());
        String comment = form.getComment() == null ? "" : form.getComment().trim();
        review.setComment(comment.isEmpty() ? null : comment);
        return reviews.save(review);
    }

    /** Authors may remove their own review; managers and admins may remove any. */
    @Transactional
    public Long delete(Long reviewId, Long actorId, Role actorRole) {
        Review review = reviews.findById(reviewId).orElseThrow(() -> new NotFoundException("Review", reviewId));
        boolean owner = review.getUser().getId().equals(actorId);
        if (!owner && actorRole.compareTo(Role.MANAGER) < 0) {
            throw new NotFoundException("Review", reviewId);
        }
        Long productId = review.getProduct().getId();
        reviews.delete(review);
        return productId;
    }

    public boolean isWished(Long userId, Long productId) {
        return wishlist.existsByUserIdAndProductId(userId, productId);
    }

    public List<WishlistItem> wishlistOf(Long userId) {
        List<WishlistItem> items = wishlist.findByUserIdOrderByCreatedAtDescIdDesc(userId);
        com.marzouki.shop.catalog.ProductViews.warm(items.stream().map(WishlistItem::getProduct).toList());
        return items;
    }

    /** Adds the product if missing, removes it otherwise. Returns true when it is now on the list. */
    @Transactional
    public boolean toggleWish(Long userId, Long productId) {
        if (wishlist.deleteByUserIdAndProductId(userId, productId) > 0) {
            return false;
        }
        products.findByIdAndActiveTrue(productId).orElseThrow(() -> new NotFoundException("Product", productId));
        WishlistItem item = new WishlistItem();
        item.setUser(users.getReferenceById(userId));
        item.setProduct(products.getReferenceById(productId));
        wishlist.save(item);
        return true;
    }
}