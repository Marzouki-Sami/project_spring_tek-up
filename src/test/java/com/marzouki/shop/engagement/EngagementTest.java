package com.marzouki.shop.engagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marzouki.shop.cart.Cart;
import com.marzouki.shop.cart.CartService;
import com.marzouki.shop.catalog.Category;
import com.marzouki.shop.catalog.CategoryRepository;
import com.marzouki.shop.catalog.Product;
import com.marzouki.shop.catalog.ProductRepository;
import com.marzouki.shop.catalog.Subcategory;
import com.marzouki.shop.catalog.SubcategoryRepository;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.NotFoundException;
import com.marzouki.shop.engagement.dto.ReviewForm;
import com.marzouki.shop.order.OrderRepository;
import com.marzouki.shop.order.OrderService;
import com.marzouki.shop.order.StockMovementRepository;
import com.marzouki.shop.order.dto.ShippingForm;
import com.marzouki.shop.security.AppUserDetails;
import com.marzouki.shop.support.TestPasswords;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserRepository;
import com.marzouki.shop.user.UserService;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** Not transactional: pages render after the service call, as in production, so lazy loading bugs show up. */
@SpringBootTest
@AutoConfigureMockMvc
class EngagementTest {

    @Autowired MockMvc mvc;
    @Autowired EngagementService engagement;
    @Autowired OrderService orders;
    @Autowired CartService carts;
    @Autowired UserService users;
    @Autowired UserRepository userRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired ProductRepository products;
    @Autowired SubcategoryRepository subcategories;
    @Autowired CategoryRepository categories;
    @Autowired StockMovementRepository movements;
    @Autowired ReviewRepository reviewRepository;
    @Autowired WishlistRepository wishlistRepository;

    User buyer;
    User stranger;
    User manager;
    Product lamp;

    @BeforeEach
    void data() {
        Category category = new Category();
        category.setTitle("Home");
        categories.save(category);
        Subcategory sub = new Subcategory();
        sub.setTitle("Lamps");
        sub.setCategory(category);
        subcategories.save(sub);
        lamp = new Product();
        lamp.setSku("L-1");
        lamp.setName("Lamp");
        lamp.setPrice(new BigDecimal("20.00"));
        lamp.addVariant("L-1", 3);
        lamp.setSubcategory(sub);
        products.save(lamp);
        buyer = users.create("Bea", "Buyer", "bea@example.com", TestPasswords.strong(), Role.CUSTOMER);
        stranger = users.create("Sam", "Stranger", "sam@example.com", TestPasswords.strong(), Role.CUSTOMER);
        manager = users.create("Mo", "Manager", "mo@example.com", TestPasswords.strong(), Role.MANAGER);

        Cart cart = new Cart();
        carts.add(cart, lamp.getDefaultVariant().getId(), 1);
        ShippingForm address = new ShippingForm();
        address.setFullName("Bea Buyer");
        address.setAddress("1 Main Street");
        address.setCity("Lyon");
        address.setPostalCode("69001");
        address.setCountry("France");
        orders.place(buyer.getId(), address, cart);
    }

    @AfterEach
    void cleanUp() {
        reviewRepository.deleteAll();
        wishlistRepository.deleteAll();
        movements.deleteAll();
        orderRepository.deleteAll();
        products.deleteAll();
        subcategories.deleteAll();
        categories.deleteAll();
        userRepository.deleteAll();
    }

    private ReviewForm review(int rating, String comment) {
        ReviewForm form = new ReviewForm();
        form.setRating(rating);
        form.setComment(comment);
        return form;
    }

    @Test
    void onlyBuyersCanReviewAndALaterReviewReplacesTheFirst() {
        assertThatThrownBy(() -> engagement.submit(stranger.getId(), lamp.getId(), review(5, "x")))
                .isInstanceOf(ConflictException.class);

        engagement.submit(buyer.getId(), lamp.getId(), review(3, "Okay"));
        engagement.submit(buyer.getId(), lamp.getId(), review(5, "  Great  "));

        assertThat(engagement.reviewsFor(lamp.getId())).singleElement().satisfies(r -> {
            assertThat(r.getRating()).isEqualTo(5);
            assertThat(r.getComment()).isEqualTo("Great");
        });
        assertThat(engagement.averageRating(lamp.getId())).isEqualTo(5.0);
    }

    @Test
    void cancelledOrdersDoNotCountAsPurchases() {
        Long orderId = orderRepository.findByUserIdOrderByCreatedAtDescIdDesc(buyer.getId()).get(0).getId();
        orders.cancelOwn(orderId, buyer.getId());
        assertThat(engagement.hasBought(buyer.getId(), lamp.getId())).isFalse();
    }

    @Test
    void authorsAndManagersMayDeleteReviewsButOthersMayNot() {
        Review review = engagement.submit(buyer.getId(), lamp.getId(), review(4, null));
        assertThatThrownBy(() -> engagement.delete(review.getId(), stranger.getId(), Role.CUSTOMER))
                .isInstanceOf(NotFoundException.class);
        engagement.delete(review.getId(), manager.getId(), Role.MANAGER);
        assertThat(engagement.reviewsFor(lamp.getId())).isEmpty();
    }

    @Test
    void theWishlistTogglesAndKeepsOneEntryPerProduct() {
        assertThat(engagement.toggleWish(buyer.getId(), lamp.getId())).isTrue();
        assertThat(engagement.wishlistOf(buyer.getId())).hasSize(1);
        assertThat(engagement.toggleWish(buyer.getId(), lamp.getId())).isFalse();
        assertThat(engagement.wishlistOf(buyer.getId())).isEmpty();
    }

    @Test
    void pagesRenderForGuestsBuyersAndManagers() throws Exception {
        engagement.submit(buyer.getId(), lamp.getId(), review(4, "Bright and sturdy"));
        engagement.toggleWish(buyer.getId(), lamp.getId());

        mvc.perform(get("/products/" + lamp.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Bright and sturdy")))
                .andExpect(content().string(containsString("Verified buyer")));
        mvc.perform(get("/products/" + lamp.getId()).with(user(new AppUserDetails(buyer)))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Update your review")));
        mvc.perform(get("/products/" + lamp.getId()).with(user(new AppUserDetails(stranger)))).andExpect(status().isOk());
        mvc.perform(get("/account/wishlist").with(user(new AppUserDetails(buyer)))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Lamp")));
        mvc.perform(get("/manage").with(user(new AppUserDetails(manager)))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Best sellers")))
                .andExpect(content().string(containsString("Lamp")));
    }

    @Test
    void reviewsAndWishlistNeedASignedInUser() throws Exception {
        mvc.perform(post("/reviews").with(csrf()).param("productId", lamp.getId().toString()).param("rating", "5"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/wishlist/toggle").with(csrf()).param("productId", lamp.getId().toString()))
                .andExpect(status().is3xxRedirection());
        assertThat(reviewRepository.count()).isZero();
        mvc.perform(get("/manage").with(user(new AppUserDetails(buyer)))).andExpect(status().isForbidden());
    }

    @Test
    void submittingAReviewThroughThePageSavesIt() throws Exception {
        mvc.perform(post("/reviews").with(user(new AppUserDetails(buyer))).with(csrf())
                        .param("productId", lamp.getId().toString()).param("rating", "5").param("comment", "Love it"))
                .andExpect(status().is3xxRedirection());
        assertThat(reviewRepository.count()).isEqualTo(1);
        mvc.perform(post("/reviews").with(user(new AppUserDetails(buyer))).with(csrf())
                        .param("productId", lamp.getId().toString()).param("rating", "9"))
                .andExpect(status().is3xxRedirection());
        assertThat(reviewRepository.findAll()).singleElement().satisfies(r -> assertThat(r.getRating()).isEqualTo(5));
    }
}