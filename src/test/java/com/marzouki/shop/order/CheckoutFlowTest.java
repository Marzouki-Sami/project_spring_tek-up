package com.marzouki.shop.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marzouki.shop.catalog.Category;
import com.marzouki.shop.catalog.CategoryRepository;
import com.marzouki.shop.catalog.Product;
import com.marzouki.shop.catalog.ProductRepository;
import com.marzouki.shop.catalog.Subcategory;
import com.marzouki.shop.catalog.SubcategoryRepository;
import com.marzouki.shop.security.AppUserDetails;
import com.marzouki.shop.support.TestPasswords;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CheckoutFlowTest {

    @Autowired MockMvc mvc;
    @Autowired UserService users;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired SubcategoryRepository subcategories;
    @Autowired OrderRepository orders;

    Product lamp;
    User customer;

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
        lamp.addVariant("L-1", 5);
        lamp.setSubcategory(sub);
        products.save(lamp);
        customer = users.create("Cy", "Customer", "cy@example.com", TestPasswords.strong(), Role.CUSTOMER);
    }

    @Test
    void guestsCanFillTheCartButCheckoutAsksThemToSignIn() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/cart/add").session(session).with(csrf()).param("variantId", lamp.getDefaultVariant().getId().toString()))
                .andExpect(redirectedUrl("/cart"));
        mvc.perform(get("/cart").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Lamp")));
        mvc.perform(get("/checkout").session(session)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void aCartIsPrivateToItsSession() throws Exception {
        mvc.perform(post("/cart/add").session(new MockHttpSession()).with(csrf())
                .param("productId", lamp.getId().toString()));
        mvc.perform(get("/cart").session(new MockHttpSession())).andExpect(content().string(containsString("Your cart is empty")));
    }

    @Test
    void aSignedInCustomerCanCheckOutAndSeeTheOrder() throws Exception {
        MockHttpSession session = new MockHttpSession();
        var me = user(new AppUserDetails(customer));
        mvc.perform(post("/cart/add").session(session).with(csrf()).param("variantId", lamp.getDefaultVariant().getId().toString())
                .param("quantity", "2"));

        mvc.perform(post("/checkout").session(session).with(me).with(csrf())
                        .param("fullName", "Cy Customer").param("address", "1 Main Street").param("city", "Lyon")
                        .param("postalCode", "69001").param("country", "France"))
                .andExpect(status().is3xxRedirection());

        Order order = orders.findByUserIdOrderByCreatedAtDescIdDesc(customer.getId()).get(0);
        assertThat(order.getTotal()).isEqualByComparingTo("40.00");
        mvc.perform(get("/account/orders/" + order.getId()).with(me)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Lamp")));
        mvc.perform(get("/cart").session(session)).andExpect(content().string(containsString("Your cart is empty")));
    }

    @Test
    void checkoutRejectsAnIncompleteAddress() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/cart/add").session(session).with(csrf()).param("variantId", lamp.getDefaultVariant().getId().toString()));
        mvc.perform(post("/checkout").session(session).with(user(new AppUserDetails(customer))).with(csrf())
                        .param("fullName", " "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Enter the street address")));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customersCannotUseTheOrderBackOffice() throws Exception {
        mvc.perform(get("/manage/orders")).andExpect(status().isForbidden());
        mvc.perform(get("/manage/coupons")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managersSeeOrdersAndCoupons() throws Exception {
        mvc.perform(get("/manage/orders")).andExpect(status().isOk());
        mvc.perform(post("/manage/coupons").with(csrf()).param("code", "WELCOME").param("percentOff", "15"))
                .andExpect(redirectedUrl("/manage/coupons"));
        mvc.perform(get("/manage/coupons")).andExpect(content().string(containsString("WELCOME")));
    }
}