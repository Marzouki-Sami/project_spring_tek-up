package com.marzouki.shop.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
import com.marzouki.shop.order.Order;
import com.marzouki.shop.order.OrderRepository;
import com.marzouki.shop.order.OrderService;
import com.marzouki.shop.order.OrderStatus;
import com.marzouki.shop.order.StockMovementRepository;
import com.marzouki.shop.order.dto.ShippingForm;
import com.marzouki.shop.support.TestPasswords;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserRepository;
import com.marzouki.shop.user.UserService;
import com.marzouki.shop.user.dto.RegistrationForm;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

/** Not transactional: mail listeners only fire once the triggering transaction has really committed. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(EmailFlowTest.Recording.class)
class EmailFlowTest {

    static final List<Email> SENT = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class Recording {
        @Bean
        @Primary
        MailOutbox recordingOutbox() {
            return SENT::add;
        }
    }

    @Autowired MockMvc mvc;
    @Autowired UserService users;
    @Autowired UserRepository userRepository;
    @Autowired OrderService orders;
    @Autowired OrderRepository orderRepository;
    @Autowired CartService carts;
    @Autowired ProductRepository products;
    @Autowired SubcategoryRepository subcategories;
    @Autowired CategoryRepository categories;
    @Autowired StockMovementRepository movements;

    @BeforeEach
    void clear() {
        SENT.clear();
    }

    @AfterEach
    void cleanUp() {
        movements.deleteAll();
        orderRepository.deleteAll();
        products.deleteAll();
        subcategories.deleteAll();
        categories.deleteAll();
        userRepository.deleteAll();
    }

    private String tokenIn(Email email) {
        Matcher matcher = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(email.text());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private User register(String email, String password) {
        RegistrationForm form = new RegistrationForm();
        form.setFirstName("Ann");
        form.setLastName("Example");
        form.setEmail(email);
        form.setPassword(password);
        form.setConfirmPassword(password);
        return users.register(form);
    }

    @Test
    void signingUpSendsAOneTimeLinkThatConfirmsTheAddress() throws Exception {
        User user = register("ann@example.com", TestPasswords.strong());
        assertThat(users.get(user.getId()).isEmailVerified()).isFalse();
        assertThat(SENT).singleElement().satisfies(mail -> {
            assertThat(mail.to()).isEqualTo("ann@example.com");
            assertThat(mail.html()).contains("Confirm email address");
        });

        String token = tokenIn(SENT.get(0));
        mvc.perform(get("/verify-email").param("token", token)).andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("success"));
        assertThat(users.get(user.getId()).isEmailVerified()).isTrue();

        mvc.perform(get("/verify-email").param("token", token)).andExpect(flash().attributeExists("error"));
    }

    @Test
    void aNewConfirmationLinkRetiresTheOldOne() {
        User user = register("bo@example.com", TestPasswords.strong());
        String first = tokenIn(SENT.get(0));
        users.resendVerification(user.getId());
        assertThat(SENT).hasSize(2);

        assertThatThrownBy(() -> users.verifyEmail(first)).isInstanceOf(ConflictException.class);
        users.verifyEmail(tokenIn(SENT.get(1)));
        assertThat(users.get(user.getId()).isEmailVerified()).isTrue();
    }

    @Test
    void resetLinksChangeThePasswordOnceAndStayQuietForUnknownAddresses() throws Exception {
        String old = TestPasswords.strong();
        register("cy@example.com", old);
        SENT.clear();

        mvc.perform(post("/forgot-password").with(csrf()).param("email", "nobody@example.com"))
                .andExpect(redirectedUrl("/login")).andExpect(flash().attributeExists("success"));
        assertThat(SENT).isEmpty();

        mvc.perform(post("/forgot-password").with(csrf()).param("email", "CY@example.com"))
                .andExpect(redirectedUrl("/login"));
        assertThat(SENT).singleElement().satisfies(mail -> assertThat(mail.subject()).contains("Reset"));
        String token = tokenIn(SENT.get(0));

        String fresh = TestPasswords.strong();
        mvc.perform(post("/reset-password").with(csrf()).param("token", token)
                        .param("newPassword", "weak").param("confirmPassword", "weak"))
                .andExpect(status().isOk());
        mvc.perform(post("/reset-password").with(csrf()).param("token", token)
                        .param("newPassword", fresh).param("confirmPassword", fresh))
                .andExpect(redirectedUrl("/login"));

        mvc.perform(post("/login").with(csrf()).param("username", "cy@example.com").param("password", fresh))
                .andExpect(redirectedUrl("/"));
        mvc.perform(post("/reset-password").with(csrf()).param("token", token)
                        .param("newPassword", fresh).param("confirmPassword", fresh))
                .andExpect(status().isOk());
    }

    private Product stockedProduct(int stock) {
        Category category = new Category();
        category.setTitle("Home");
        categories.save(category);
        Subcategory sub = new Subcategory();
        sub.setTitle("Lamps");
        sub.setCategory(category);
        subcategories.save(sub);
        Product lamp = new Product();
        lamp.setSku("L-1");
        lamp.setName("Lamp");
        lamp.setPrice(new BigDecimal("20.00"));
        lamp.addVariant("L-1", stock);
        lamp.setSubcategory(sub);
        return products.save(lamp);
    }

    private Order order(User buyer, Product product, int quantity) {
        Cart cart = new Cart();
        carts.add(cart, product.getDefaultVariant().getId(), quantity);
        ShippingForm address = new ShippingForm();
        address.setFullName("Bea Buyer");
        address.setAddress("1 Main Street");
        address.setCity("Lyon");
        address.setPostalCode("69001");
        address.setCountry("France");
        return orders.place(buyer.getId(), address, cart);
    }

    @Test
    void ordersSendConfirmationAndShippingMailsAndLowStockAlertsToStaffOnlyOnce() {
        User buyer = users.create("Bea", "Buyer", "bea@example.com", TestPasswords.strong(), Role.CUSTOMER);
        users.create("Mo", "Manager", "mo@example.com", TestPasswords.strong(), Role.MANAGER);
        Product lamp = stockedProduct(7);

        Order order = order(buyer, lamp, 2);
        assertThat(SENT).extracting(Email::to).containsExactlyInAnyOrder("bea@example.com", "mo@example.com");
        Email confirmation = SENT.stream().filter(m -> m.to().equals("bea@example.com")).findFirst().orElseThrow();
        assertThat(confirmation.subject()).contains("confirmed");
        assertThat(confirmation.html()).contains("Lamp").contains("40.00");
        assertThat(SENT.stream().filter(m -> m.to().equals("mo@example.com")).findFirst().orElseThrow().subject())
                .contains("Low stock");

        SENT.clear();
        order(buyer, lamp, 1);
        assertThat(SENT).extracting(Email::to).containsExactly("bea@example.com");

        SENT.clear();
        orders.changeStatus(order.getId(), OrderStatus.PROCESSING);
        assertThat(SENT).isEmpty();
        orders.changeStatus(order.getId(), OrderStatus.SHIPPED);
        assertThat(SENT).singleElement().satisfies(mail -> assertThat(mail.subject()).contains("on its way"));
    }

    @Test
    void aFailedOrderSendsNothing() {
        User buyer = users.create("Bea", "Buyer", "bea@example.com", TestPasswords.strong(), Role.CUSTOMER);
        stockedProduct(1);
        assertThatThrownBy(() -> orders.place(buyer.getId(), new ShippingForm(), new Cart()))
                .isInstanceOf(RuntimeException.class);
        assertThat(SENT).isEmpty();
    }
}