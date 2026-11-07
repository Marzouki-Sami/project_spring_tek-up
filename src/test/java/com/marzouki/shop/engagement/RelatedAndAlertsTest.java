package com.marzouki.shop.engagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marzouki.shop.cart.Cart;
import com.marzouki.shop.cart.CartService;
import com.marzouki.shop.cart.CartStore;
import com.marzouki.shop.catalog.CategoryRepository;
import com.marzouki.shop.catalog.CategoryService;
import com.marzouki.shop.catalog.Product;
import com.marzouki.shop.catalog.ProductRepository;
import com.marzouki.shop.catalog.ProductService;
import com.marzouki.shop.catalog.RelatedProducts;
import com.marzouki.shop.catalog.SubcategoryRepository;
import com.marzouki.shop.catalog.SubcategoryService;
import com.marzouki.shop.catalog.VariantService;
import com.marzouki.shop.catalog.dto.CategoryForm;
import com.marzouki.shop.catalog.dto.ProductForm;
import com.marzouki.shop.catalog.dto.SubcategoryForm;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.mail.Email;
import com.marzouki.shop.mail.MailOutbox;
import com.marzouki.shop.order.Order;
import com.marzouki.shop.order.OrderRepository;
import com.marzouki.shop.order.OrderService;
import com.marzouki.shop.order.OrderStatus;
import com.marzouki.shop.order.StockMovement;
import com.marzouki.shop.order.StockMovementRepository;
import com.marzouki.shop.order.StockService;
import com.marzouki.shop.order.dto.ShippingForm;
import com.marzouki.shop.security.AppUserDetails;
import com.marzouki.shop.support.TestPasswords;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserRepository;
import com.marzouki.shop.user.UserService;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RelatedAndAlertsTest.Recording.class)
class RelatedAndAlertsTest {

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
    @Autowired ProductService productService;
    @Autowired VariantService variantService;
    @Autowired RelatedProducts related;
    @Autowired CategoryService categoryService;
    @Autowired SubcategoryService subcategoryService;
    @Autowired ProductRepository productRepository;
    @Autowired SubcategoryRepository subcategories;
    @Autowired CategoryRepository categories;
    @Autowired OrderService orders;
    @Autowired OrderRepository orderRepository;
    @Autowired CartService carts;
    @Autowired CartStore cartStore;
    @Autowired EngagementService engagement;
    @Autowired StockService stock;
    @Autowired StockMovementRepository movements;
    @Autowired JdbcTemplate jdbc;

    Long tops;
    Long bags;
    Product tee;
    Product shirt;
    Product bag;
    Product hat;
    Product hidden;
    User manager;
    User ann;
    User bob;
    User cy;

    @BeforeEach
    void data() {
        SENT.clear();
        related.refreshPurchases();
        CategoryForm category = new CategoryForm();
        category.setTitle("Fashion");
        Long categoryId = categoryService.create(category).getId();
        tops = subcategory("Tops", categoryId);
        bags = subcategory("Bags", categoryId);
        tee = product("TEE", "Tee", tops, 5, true);
        shirt = product("SHIRT", "Shirt", tops, 5, true);
        bag = product("BAG", "Bag", bags, 5, true);
        hat = product("HAT", "Hat", bags, 5, true);
        hidden = product("HID", "Hidden thing", tops, 5, false);
        manager = users.create("Mo", "Manager", "mo@example.com", TestPasswords.strong(), Role.MANAGER);
        ann = users.create("Ann", "A", "ann@example.com", TestPasswords.strong(), Role.CUSTOMER);
        bob = users.create("Bob", "B", "bob@example.com", TestPasswords.strong(), Role.CUSTOMER);
        cy = users.create("Cy", "C", "cy@example.com", TestPasswords.strong(), Role.CUSTOMER);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from stock_alert_log");
        movements.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        subcategories.deleteAll();
        categories.deleteAll();
        userRepository.deleteAll();
    }

    private Long subcategory(String title, Long categoryId) {
        SubcategoryForm form = new SubcategoryForm();
        form.setTitle(title);
        form.setCategoryId(categoryId);
        return subcategoryService.create(form).getId();
    }

    private Product product(String sku, String name, Long subcategoryId, int stockCount, boolean active) {
        ProductForm form = new ProductForm();
        form.setSku(sku);
        form.setName(name);
        form.setPrice(new BigDecimal("10.00"));
        form.setStock(stockCount);
        form.setSubcategoryId(subcategoryId);
        form.setActive(active);
        return productService.create(form);
    }

    private Long variantOf(Product product) {
        return variantService.productWithVariants(product.getId()).getDefaultVariant().getId();
    }

    private Order buy(User who, Product... items) {
        Cart cart = new Cart();
        for (Product p : items) {
            carts.add(cart, variantOf(p), 1);
        }
        ShippingForm address = new ShippingForm();
        address.setFullName(who.getFullName());
        address.setAddress("1 Main Street");
        address.setCity("Lyon");
        address.setPostalCode("69001");
        address.setCountry("France");
        return orders.place(who.getId(), address, cart);
    }

    private List<String> names(List<Product> list) {
        return list.stream().map(Product::getName).toList();
    }

    @Test
    void relatedProductsComeFromPicksThenPurchasesThenTheSameSubcategory() {
        assertThat(names(related.forProduct(tee.getId()))).containsExactly("Shirt");

        related.add(tee.getId(), "bag");
        assertThat(names(related.forProduct(tee.getId()))).containsExactly("Bag", "Shirt");

        buy(ann, tee, hat);
        buy(bob, tee, hat);
        buy(cy, tee, bag);
        related.remove(tee.getId(), bag.getId());
        related.refreshPurchases();
        assertThat(names(related.forProduct(tee.getId()))).containsExactly("Hat", "Bag", "Shirt");

        related.add(tee.getId(), bag.getSku());
        assertThat(names(related.forProduct(tee.getId()))).containsExactly("Bag", "Hat", "Shirt");
        assertThat(names(related.forProduct(tee.getId()))).doesNotContain("Tee", "Hidden thing");
    }

    @Test
    void cancelledOrdersDoNotCountAsPurchasedTogether() {
        Order order = buy(ann, tee, hat);
        orders.changeStatus(order.getId(), OrderStatus.CANCELLED);
        related.refreshPurchases();
        assertThat(names(related.forProduct(tee.getId()))).doesNotContain("Hat");
    }

    @Test
    void picksAreValidated() {
        assertThatThrownBy(() -> related.add(tee.getId(), "NOPE")).isInstanceOf(ConflictException.class).hasMessageContaining("NOPE");
        assertThatThrownBy(() -> related.add(tee.getId(), "tee")).isInstanceOf(ConflictException.class).hasMessageContaining("itself");
        related.add(tee.getId(), "SHIRT");
        assertThatThrownBy(() -> related.add(tee.getId(), "shirt")).isInstanceOf(ConflictException.class).hasMessageContaining("already");
        related.add(tee.getId(), "HID");
        assertThat(names(related.forProduct(tee.getId()))).doesNotContain("Hidden thing");
    }

    @Test
    void managersEditPicksAndCustomersSeeSuggestions() throws Exception {
        var asManager = user(new AppUserDetails(manager));
        mvc.perform(post("/manage/products/" + tee.getId() + "/related").with(asManager).with(csrf()).param("sku", "BAG"))
                .andExpect(flash().attributeExists("success"));
        mvc.perform(post("/manage/products/" + tee.getId() + "/related").with(asManager).with(csrf()).param("sku", "BAG"))
                .andExpect(flash().attributeExists("error"));
        mvc.perform(get("/manage/products/" + tee.getId() + "/related").with(asManager)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Your picks"))).andExpect(content().string(containsString("Bag")));
        mvc.perform(get("/products/" + tee.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("You may also like"))).andExpect(content().string(containsString("Bag")))
                .andExpect(content().string(not(containsString("Hidden thing"))));
        mvc.perform(get("/manage/products/" + tee.getId() + "/related").with(user(new AppUserDetails(ann)))).andExpect(status().isForbidden());
        mvc.perform(post("/manage/products/" + tee.getId() + "/related/" + bag.getId() + "/delete").with(asManager).with(csrf()))
                .andExpect(flash().attributeExists("success"));
        assertThat(related.picked(tee.getId())).isEmpty();
    }

    @Test
    void peopleWhoWaitedAreToldWhenAnItemIsBackAndOnlyOncePerWeek() {
        Long lampVariant = variantOf(tee);
        stock.adjust(lampVariant, -5, StockMovement.Reason.DAMAGED, null);

        Cart ansCart = new Cart();
        ansCart.set(lampVariant, 1);
        cartStore.save(ann.getId(), ansCart);
        engagement.toggleWish(bob.getId(), tee.getId());
        // cy waits for nothing, so is not told

        SENT.clear();
        stock.adjust(lampVariant, 10, StockMovement.Reason.SUPPLIER_DELIVERY, null);
        assertThat(SENT).extracting(Email::to).containsExactlyInAnyOrder("ann@example.com", "bob@example.com");
        assertThat(SENT).allSatisfy(mail -> {
            assertThat(mail.subject()).isEqualTo("Tee is back in stock");
            assertThat(mail.text()).contains("/products/" + tee.getId());
        });

        SENT.clear();
        stock.adjust(lampVariant, -10, StockMovement.Reason.DAMAGED, null);
        stock.adjust(lampVariant, 10, StockMovement.Reason.SUPPLIER_DELIVERY, null);
        assertThat(SENT).isEmpty();

        jdbc.update("update stock_alert_log set sent_at = ?", java.sql.Timestamp.from(java.time.Instant.now().minus(8, java.time.temporal.ChronoUnit.DAYS)));
        stock.adjust(lampVariant, -10, StockMovement.Reason.DAMAGED, null);
        stock.adjust(lampVariant, 10, StockMovement.Reason.SUPPLIER_DELIVERY, null);
        assertThat(SENT).hasSize(2);
    }

    @Test
    void optingOutOrDisablingTheAccountStopsTheAlerts() {
        Long variant = variantOf(tee);
        stock.adjust(variant, -5, StockMovement.Reason.DAMAGED, null);
        Cart cart = new Cart();
        cart.set(variant, 1);
        cartStore.save(ann.getId(), cart);
        cartStore.save(bob.getId(), cart);

        var form = users.profileFor(ann.getId());
        form.setBackInStockAlerts(false);
        users.updateProfile(ann.getId(), form);
        users.setEnabled(manager.getId(), bob.getId(), false);

        SENT.clear();
        stock.adjust(variant, 5, StockMovement.Reason.SUPPLIER_DELIVERY, null);
        assertThat(SENT).isEmpty();
    }

    @Test
    void anOrderCancelledWhileSoldOutAlsoCountsAsRestock() {
        Long variant = variantOf(hat);
        Order order = buy(ann, hat);
        stock.adjust(variant, -4, StockMovement.Reason.DAMAGED, null);
        assertThat(variantService.getForStock(variant).getStock()).isZero();
        Cart cart = new Cart();
        cart.set(variant, 1);
        cartStore.save(cy.getId(), cart);

        SENT.clear();
        orders.changeStatus(order.getId(), OrderStatus.CANCELLED);
        assertThat(SENT).extracting(Email::to).containsExactly("cy@example.com");
    }

    @Test
    void inactiveItemsAreNotAnnounced() {
        Long variant = variantOf(hidden);
        stock.adjust(variant, -5, StockMovement.Reason.DAMAGED, null);
        Cart cart = new Cart();
        cart.set(variant, 1);
        cartStore.save(ann.getId(), cart);
        SENT.clear();
        stock.adjust(variant, 5, StockMovement.Reason.SUPPLIER_DELIVERY, null);
        assertThat(SENT).isEmpty();
    }
}
