package com.marzouki.shop.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marzouki.shop.catalog.Category;
import com.marzouki.shop.catalog.CategoryRepository;
import com.marzouki.shop.catalog.Product;
import com.marzouki.shop.catalog.ProductRepository;
import com.marzouki.shop.catalog.Subcategory;
import com.marzouki.shop.catalog.SubcategoryRepository;
import com.marzouki.shop.order.CouponRepository;
import com.marzouki.shop.order.OrderService;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Not transactional: the basket is saved by an interceptor after each request, which must really commit. */
@SpringBootTest
@AutoConfigureMockMvc
class SavedCartTest {

    @Autowired MockMvc mvc;
    @Autowired CartStore store;
    @Autowired UserService users;
    @Autowired UserRepository userRepository;
    @Autowired SavedCartRepository savedCarts;
    @Autowired ProductRepository products;
    @Autowired SubcategoryRepository subcategories;
    @Autowired CategoryRepository categories;
    @Autowired CouponRepository coupons;
    @Autowired OrderService orders;

    Product lamp;
    Product desk;
    User customer;
    String password;

    @BeforeEach
    void data() {
        Category category = new Category();
        category.setTitle("Home");
        categories.save(category);
        Subcategory sub = new Subcategory();
        sub.setTitle("Lamps");
        sub.setCategory(category);
        subcategories.save(sub);
        lamp = product("L-1", "Lamp", 5, sub);
        desk = product("D-1", "Desk", 2, sub);
        password = TestPasswords.strong();
        customer = users.create("Cy", "Customer", "cy@example.com", password, Role.CUSTOMER);
    }

    @AfterEach
    void cleanUp() {
        savedCarts.deleteAll();
        coupons.deleteAll();
        products.deleteAll();
        subcategories.deleteAll();
        categories.deleteAll();
        userRepository.deleteAll();
    }

    private Product product(String sku, String name, int stock, Subcategory sub) {
        Product p = new Product();
        p.setSku(sku);
        p.setName(name);
        p.setPrice(new BigDecimal("10.00"));
        p.addVariant(sku, stock);
        p.setSubcategory(sub);
        return products.save(p);
    }

    private Long variantId(Product p) {
        return p.getDefaultVariant().getId();
    }

    private MockHttpServletRequestBuilder add(Product p, int quantity, MockHttpSession session) {
        return post("/cart/add").session(session).with(csrf()).param("variantId", variantId(p).toString())
                .param("quantity", String.valueOf(quantity));
    }

    private MockHttpSession signIn(MockHttpSession guestSession) throws Exception {
        MockHttpServletRequestBuilder login = post("/login").with(csrf()).param("username", "cy@example.com")
                .param("password", password);
        if (guestSession != null) {
            login.session(guestSession);
        }
        return (MockHttpSession) mvc.perform(login).andReturn().getRequest().getSession();
    }

    @Test
    void aGuestBasketIsMergedIntoTheSavedOneAtSignInAndKeptForNextTime() throws Exception {
        Cart saved = new Cart();
        saved.set(variantId(lamp), 2);
        store.save(customer.getId(), saved);

        MockHttpSession guest = new MockHttpSession();
        mvc.perform(add(lamp, 1, guest));
        mvc.perform(add(desk, 1, guest));

        MockHttpSession session = signIn(guest);
        mvc.perform(get("/cart").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Lamp"))).andExpect(content().string(containsString("Desk")));

        Cart stored = store.load(customer.getId());
        assertThat(stored.quantityOf(variantId(lamp))).isEqualTo(3);
        assertThat(stored.quantityOf(variantId(desk))).isEqualTo(1);

        // A completely new session (another device, or after signing out) gets the same basket back.
        MockHttpSession later = signIn(null);
        mvc.perform(get("/cart").session(later)).andExpect(content().string(containsString("Desk")));
    }

    @Test
    void mergedQuantitiesNeverExceedTheStock() throws Exception {
        Cart saved = new Cart();
        saved.set(variantId(desk), 2);
        store.save(customer.getId(), saved);
        MockHttpSession guest = new MockHttpSession();
        mvc.perform(add(desk, 2, guest));

        signIn(guest);
        assertThat(store.load(customer.getId()).quantityOf(variantId(desk))).isEqualTo(2);
    }

    @Test
    void changesMadeWhileSignedInAreSavedAndCheckoutEmptiesTheSavedBasket() throws Exception {
        MockHttpSession session = signIn(null);
        mvc.perform(add(lamp, 2, session));
        assertThat(store.load(customer.getId()).quantityOf(variantId(lamp))).isEqualTo(2);

        mvc.perform(post("/cart/update").session(session).with(csrf())
                .param("variantId", variantId(lamp).toString()).param("quantity", "4"));
        assertThat(store.load(customer.getId()).quantityOf(variantId(lamp))).isEqualTo(4);

        mvc.perform(post("/cart/remove").session(session).with(csrf()).param("variantId", variantId(lamp).toString()));
        assertThat(savedCarts.findByUserId(customer.getId())).isEmpty();
    }

    @Test
    void theCouponIsSavedWithTheBasket() throws Exception {
        orders.createCoupon("SAVE10", 10, null, null);
        MockHttpSession session = signIn(null);
        mvc.perform(add(lamp, 1, session));
        mvc.perform(post("/cart/coupon").session(session).with(csrf()).param("code", "save10"));

        assertThat(store.load(customer.getId()).getCouponCode()).isEqualTo("SAVE10");
    }

    @Test
    void guestsNeverGetADatabaseBasket() throws Exception {
        MockHttpSession guest = new MockHttpSession();
        mvc.perform(add(lamp, 1, guest));
        assertThat(savedCarts.count()).isZero();
    }

    @Test
    void registeringKeepsTheGuestBasket() throws Exception {
        MockHttpSession guest = new MockHttpSession();
        mvc.perform(add(lamp, 2, guest));
        String strong = TestPasswords.strong();
        mvc.perform(post("/register").session(guest).with(csrf()).param("firstName", "Ann").param("lastName", "New")
                .param("email", "ann@example.com").param("password", strong).param("confirmPassword", strong));

        User ann = userRepository.findByEmailIgnoreCase("ann@example.com").orElseThrow();
        assertThat(store.load(ann.getId()).quantityOf(variantId(lamp))).isEqualTo(2);
    }

    @Test
    void abandonedBasketsAreCleanedUpButRecentOnesStay() {
        Cart cart = new Cart();
        cart.set(variantId(lamp), 1);
        store.save(customer.getId(), cart);
        assertThat(store.deleteStale()).isZero();

        SavedCart stored = savedCarts.findByUserId(customer.getId()).orElseThrow();
        stored.setUpdatedAt(java.time.Instant.now().minusSeconds(90L * 24 * 3600));
        savedCarts.save(stored);
        assertThat(store.deleteStale()).isEqualTo(1);
    }
}