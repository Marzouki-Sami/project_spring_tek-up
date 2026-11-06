package com.marzouki.shop.order;

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
import com.marzouki.shop.order.dto.ShippingForm;
import com.marzouki.shop.security.AppUserDetails;
import com.marzouki.shop.support.TestPasswords;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserService;
import com.marzouki.shop.user.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Not transactional on purpose: pages are rendered after the service call has finished, exactly as
 * in production (open-in-view is off), so a lazily loaded association would fail here.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderPagesTest {

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

    @AfterEach
    void cleanUp() {
        movements.deleteAll();
        orderRepository.deleteAll();
        products.deleteAll();
        subcategories.deleteAll();
        categories.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void cartAndOrderPagesRenderWithoutALiveSession() throws Exception {
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
        lamp.setStock(5);
        lamp.setSubcategory(sub);
        products.save(lamp);
        User customer = users.create("Cy", "Customer", "cy@example.com", TestPasswords.strong(), Role.CUSTOMER);
        User manager = users.create("Mo", "Manager", "mo@example.com", TestPasswords.strong(), Role.MANAGER);

        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/cart/add").session(session).with(csrf()).param("productId", lamp.getId().toString()));
        mvc.perform(get("/cart").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Lamp")));

        Cart cart = new Cart();
        carts.add(cart, lamp.getId(), 1);
        ShippingForm address = new ShippingForm();
        address.setFullName("Cy Customer");
        address.setAddress("1 Main Street");
        address.setCity("Lyon");
        address.setPostalCode("69001");
        address.setCountry("France");
        Order order = orders.place(customer.getId(), address, cart);

        mvc.perform(get("/account/orders").with(user(new AppUserDetails(customer)))).andExpect(status().isOk());
        mvc.perform(get("/account/orders/" + order.getId()).with(user(new AppUserDetails(customer))))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Lamp")));
        mvc.perform(get("/manage/orders").with(user(new AppUserDetails(manager)))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Cy Customer")));
        mvc.perform(get("/manage/orders/" + order.getId()).with(user(new AppUserDetails(manager))))
                .andExpect(status().isOk()).andExpect(content().string(containsString("cy@example.com")));
        mvc.perform(get("/account/orders/" + order.getId()).with(user(new AppUserDetails(manager))))
                .andExpect(status().isNotFound());
    }
}