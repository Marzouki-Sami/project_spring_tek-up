package com.marzouki.shop.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marzouki.shop.cart.Cart;
import com.marzouki.shop.cart.CartService;
import com.marzouki.shop.catalog.CategoryRepository;
import com.marzouki.shop.catalog.CategoryService;
import com.marzouki.shop.catalog.Product;
import com.marzouki.shop.catalog.ProductRepository;
import com.marzouki.shop.catalog.ProductService;
import com.marzouki.shop.catalog.SubcategoryRepository;
import com.marzouki.shop.catalog.SubcategoryService;
import com.marzouki.shop.catalog.VariantService;
import com.marzouki.shop.catalog.dto.CategoryForm;
import com.marzouki.shop.catalog.dto.ProductForm;
import com.marzouki.shop.catalog.dto.SubcategoryForm;
import com.marzouki.shop.order.Order;
import com.marzouki.shop.order.OrderRepository;
import com.marzouki.shop.order.OrderService;
import com.marzouki.shop.order.OrderStatus;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerListTest {

    @Autowired MockMvc mvc;
    @Autowired UserService users;
    @Autowired UserRepository userRepository;
    @Autowired CustomerRepository customers;
    @Autowired OrderService orders;
    @Autowired OrderRepository orderRepository;
    @Autowired CartService carts;
    @Autowired ProductService productService;
    @Autowired VariantService variantService;
    @Autowired CategoryService categoryService;
    @Autowired SubcategoryService subcategoryService;
    @Autowired ProductRepository productRepository;
    @Autowired SubcategoryRepository subcategories;
    @Autowired CategoryRepository categories;
    @Autowired StockMovementRepository movements;

    User manager;
    User anna;
    User ben;

    @BeforeEach
    void data() {
        manager = users.create("Mo", "Manager", "mo@example.com", TestPasswords.strong(), Role.MANAGER);
        anna = users.create("Anna", "Zed", "anna@example.com", TestPasswords.strong(), Role.CUSTOMER);
        ben = users.create("Ben", "Alpha", "ben@example.com", TestPasswords.strong(), Role.CUSTOMER);

        CategoryForm category = new CategoryForm();
        category.setTitle("Home");
        Long categoryId = categoryService.create(category).getId();
        SubcategoryForm sub = new SubcategoryForm();
        sub.setTitle("Lamps");
        sub.setCategoryId(categoryId);
        Long subId = subcategoryService.create(sub).getId();
        ProductForm form = new ProductForm();
        form.setName("Lamp");
        form.setPrice(new BigDecimal("10.00"));
        form.setStock(50);
        form.setSubcategoryId(subId);
        Product lamp = productService.create(form);
        Long variantId = variantService.productWithVariants(lamp.getId()).getDefaultVariant().getId();

        order(anna, variantId, 2);
        Order cancelled = order(anna, variantId, 5);
        orders.changeStatus(cancelled.getId(), OrderStatus.CANCELLED);
    }

    @AfterEach
    void cleanUp() {
        movements.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        subcategories.deleteAll();
        categories.deleteAll();
        userRepository.deleteAll();
    }

    private Order order(User buyer, Long variantId, int quantity) {
        Cart cart = new Cart();
        carts.add(cart, variantId, quantity);
        ShippingForm address = new ShippingForm();
        address.setFullName(buyer.getFullName());
        address.setAddress("1 Main Street");
        address.setCity("Lyon");
        address.setPostalCode("69001");
        address.setCountry("France");
        return orders.place(buyer.getId(), address, cart);
    }

    @Test
    void onlyCustomersAreListedWithTheirTotalsAndCancelledOrdersDoNotCount() {
        var page = customers.byName("", PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(CustomerRow::email).containsExactly("ben@example.com", "anna@example.com");
        CustomerRow annaRow = page.getContent().get(1);
        assertThat(annaRow.orders()).isEqualTo(1);
        assertThat(annaRow.spent()).isEqualByComparingTo("20.00");
        assertThat(page.getContent().get(0).orders()).isZero();
        assertThat(page.getContent().get(0).lastOrder()).isNull();
        assertThat(customers.bySpent("", PageRequest.of(0, 10)).getContent().get(0).email()).isEqualTo("anna@example.com");
        assertThat(customers.byOrders("", PageRequest.of(0, 10)).getContent().get(0).email()).isEqualTo("anna@example.com");
        assertThat(customers.byNewest("", PageRequest.of(0, 10)).getTotalElements()).isEqualTo(2);
    }

    @Test
    void searchEscapesWildcards() {
        assertThat(customers.byName("%anna%", PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
        assertThat(customers.byName("%\\%%", PageRequest.of(0, 10)).getTotalElements()).isZero();
        assertThat(customers.byName("%anna zed%", PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
    }

    @Test
    void managersSeeThePagesAndNoStaffAppear() throws Exception {
        var asManager = user(new AppUserDetails(manager));
        mvc.perform(get("/manage/customers").with(asManager)).andExpect(status().isOk())
                .andExpect(content().string(containsString("anna@example.com")))
                .andExpect(content().string(not(containsString("mo@example.com"))));
        mvc.perform(get("/manage/customers").param("q", "ben").param("sort", "spent").with(asManager))
                .andExpect(content().string(containsString("ben@example.com")))
                .andExpect(content().string(not(containsString("anna@example.com"))));
        mvc.perform(get("/manage/customers").param("sort", "bogus").param("size", "7").param("page", "-3").with(asManager))
                .andExpect(status().isOk());
        mvc.perform(get("/manage/customers/" + anna.getId()).with(asManager)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Anna Zed")));
        mvc.perform(get("/manage/customers/" + manager.getId()).with(asManager)).andExpect(status().isNotFound());
    }

    @Test
    void customersCannotSeeTheCustomerList() throws Exception {
        mvc.perform(get("/manage/customers").with(user(new AppUserDetails(anna)))).andExpect(status().isForbidden());
        mvc.perform(get("/manage/customers/" + ben.getId()).with(user(new AppUserDetails(anna)))).andExpect(status().isForbidden());
    }
}
