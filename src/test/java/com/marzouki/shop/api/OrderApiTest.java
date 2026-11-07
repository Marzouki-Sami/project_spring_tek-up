package com.marzouki.shop.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marzouki.shop.address.AddressService;
import com.marzouki.shop.address.dto.AddressForm;
import com.marzouki.shop.catalog.CategoryRepository;
import com.marzouki.shop.catalog.CategoryService;
import com.marzouki.shop.catalog.ProductRepository;
import com.marzouki.shop.catalog.ProductService;
import com.marzouki.shop.catalog.SubcategoryRepository;
import com.marzouki.shop.catalog.SubcategoryService;
import com.marzouki.shop.catalog.VariantService;
import com.marzouki.shop.catalog.dto.CategoryForm;
import com.marzouki.shop.catalog.dto.ProductForm;
import com.marzouki.shop.catalog.dto.SubcategoryForm;
import com.marzouki.shop.order.CouponRepository;
import com.marzouki.shop.order.IdempotencyKeyRepository;
import com.marzouki.shop.order.OrderRepository;
import com.marzouki.shop.order.OrderService;
import com.marzouki.shop.order.StockMovementRepository;
import com.marzouki.shop.support.TestPasswords;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserRepository;
import com.marzouki.shop.user.UserService;
import java.math.BigDecimal;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Not transactional: orders are placed and committed the way real requests are. */
@SpringBootTest
@AutoConfigureMockMvc
class OrderApiTest {

    @Autowired MockMvc mvc;
    @Autowired UserService users;
    @Autowired UserRepository userRepository;
    @Autowired ProductService productService;
    @Autowired VariantService variantService;
    @Autowired CategoryService categoryService;
    @Autowired SubcategoryService subcategoryService;
    @Autowired OrderService orders;
    @Autowired OrderRepository orderRepository;
    @Autowired IdempotencyKeyRepository keys;
    @Autowired CouponRepository coupons;
    @Autowired AddressService addresses;
    @Autowired ProductRepository productRepository;
    @Autowired SubcategoryRepository subcategories;
    @Autowired CategoryRepository categories;
    @Autowired StockMovementRepository movements;

    User ann;
    User bob;
    User manager;
    Long variantId;
    Long lampId;

    @BeforeEach
    void data() {
        ann = users.create("Ann", "A", "ann@example.com", TestPasswords.strong(), Role.CUSTOMER);
        bob = users.create("Bob", "B", "bob@example.com", TestPasswords.strong(), Role.CUSTOMER);
        manager = users.create("Mo", "Manager", "mo@example.com", TestPasswords.strong(), Role.MANAGER);
        CategoryForm category = new CategoryForm();
        category.setTitle("Home");
        SubcategoryForm sub = new SubcategoryForm();
        sub.setTitle("Lamps");
        sub.setCategoryId(categoryService.create(category).getId());
        ProductForm form = new ProductForm();
        form.setName("Lamp");
        form.setPrice(new BigDecimal("10.00"));
        form.setStock(10);
        form.setSubcategoryId(subcategoryService.create(sub).getId());
        lampId = productService.create(form).getId();
        variantId = variantService.productWithVariants(lampId).getDefaultVariant().getId();
    }

    @AfterEach
    void cleanUp() {
        keys.deleteAll();
        movements.deleteAll();
        orderRepository.deleteAll();
        coupons.deleteAll();
        productRepository.deleteAll();
        subcategories.deleteAll();
        categories.deleteAll();
        userRepository.deleteAll();
    }

    private RequestPostProcessor as(User who) {
        return jwt().jwt(j -> j.subject(String.valueOf(who.getId())))
                .authorities(new SimpleGrantedAuthority("ROLE_" + who.getRole().name()));
    }

    private String order(int quantity, String extra) {
        return """
                {"items":[{"variantId":%d,"quantity":%d}],
                 "shipping":{"fullName":"Ann A","address":"1 Main Street","city":"Lyon","postalCode":"69001","country":"France"}%s}
                """.formatted(variantId, quantity, extra);
    }

    private int stock() {
        return variantService.getForStock(variantId).getStock();
    }

    private org.springframework.test.web.servlet.ResultActions place(User who, String key, String body) throws Exception {
        var request = post("/api/orders").with(as(who)).contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(key == null ? request : request.header("Idempotency-Key", key));
    }

    @Test
    void placingAnOrderReservesStockAndReturnsItsDetails() throws Exception {
        place(ann, "key-00000001", order(3, ",\"expectedTotal\":30.00")).andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.total").value(30.00))
                .andExpect(jsonPath("$.items[0].name").value("Lamp"))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.items[0].lineTotal").value(30.00))
                .andExpect(jsonPath("$.shipping.city").value("Lyon"))
                .andExpect(jsonPath("$.customerEmail").doesNotExist());
        assertThat(stock()).isEqualTo(7);
    }

    @Test
    void theSameKeyAndBodyReturnsTheSameOrderWithoutOrderingTwice() throws Exception {
        String first = place(ann, "key-00000002", order(2, "")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String second = place(ann, "key-00000002", order(2, "")).andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true")).andReturn().getResponse().getContentAsString();
        assertThat(second).isEqualTo(first);
        assertThat(stock()).isEqualTo(8);
        assertThat(orderRepository.count()).isEqualTo(1);

        place(ann, "key-00000002", order(3, "")).andExpect(status().isConflict());
        assertThat(stock()).isEqualTo(8);
        place(bob, "key-00000002", order(1, "")).andExpect(status().isCreated());
        assertThat(orderRepository.count()).isEqualTo(2);
    }

    @Test
    void parallelRetriesOfOneKeyCreateOneOrder() throws Exception {
        int attempts = 6;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch go = new CountDownLatch(1);
        try {
            java.util.List<Future<Integer>> results = new java.util.ArrayList<>();
            for (int i = 0; i < attempts; i++) {
                Callable<Integer> call = () -> {
                    go.await();
                    return place(ann, "key-parallel-1", order(1, "")).andReturn().getResponse().getStatus();
                };
                results.add(pool.submit(call));
            }
            go.countDown();
            for (Future<Integer> result : results) {
                assertThat(result.get()).isIn(200, 201, 409);
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(stock()).isEqualTo(9);
    }

    @Test
    void theKeyIsRequiredAndChecked() throws Exception {
        place(ann, null, order(1, "")).andExpect(status().isBadRequest());
        place(ann, "short", order(1, "")).andExpect(status().isBadRequest());
        place(ann, "bad key with spaces!!", order(1, "")).andExpect(status().isBadRequest());
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void refusedOrdersChangeNothing() throws Exception {
        place(ann, "key-00000003", order(11, "")).andExpect(status().isConflict());
        place(ann, "key-00000004", order(21, "")).andExpect(status().isBadRequest());
        place(ann, "key-00000005", order(1, ",\"expectedTotal\":9.00")).andExpect(status().isConflict());
        place(ann, "key-00000006", order(1, ",\"couponCode\":\"NOPE\"")).andExpect(status().isConflict());
        place(ann, "key-00000007", "{\"items\":[],\"shipping\":{\"fullName\":\"A\"}}").andExpect(status().isBadRequest());
        place(ann, "key-00000008", """
                {"items":[{"variantId":%d,"quantity":1}],"shipping":{"fullName":"Ann A"}}
                """.formatted(variantId)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.address").exists());
        place(ann, "key-00000009", "{\"items\":[{\"variantId\":999999,\"quantity\":1}],\"shipping\":{\"addressId\":5}}")
                .andExpect(status().is4xxClientError());
        assertThat(stock()).isEqualTo(10);
        assertThat(orderRepository.count()).isZero();
        assertThat(keys.count()).isZero();
    }

    @Test
    void couponsAndSavedAddressesWork() throws Exception {
        orders.createCoupon("SAVE10", 10, null, null);
        AddressForm form = new AddressForm();
        form.setLabel("Home");
        form.setFullName("Ann A");
        form.setAddress("9 Saved Road");
        form.setCity("Paris");
        form.setPostalCode("75001");
        form.setCountry("France");
        Long addressId = addresses.create(ann.getId(), form).getId();

        String body = """
                {"items":[{"variantId":%d,"quantity":2}],"couponCode":"save10","shipping":{"addressId":%d}}
                """.formatted(variantId, addressId);
        place(ann, "key-00000010", body).andExpect(status().isCreated())
                .andExpect(jsonPath("$.discount").value(2.00)).andExpect(jsonPath("$.total").value(18.00))
                .andExpect(jsonPath("$.shipping.city").value("Paris"));
        place(bob, "key-00000011", body).andExpect(status().isNotFound());
    }

    @Test
    void customersSeeOnlyTheirOwnOrdersAndCanCancelWhilePlaced() throws Exception {
        String created = place(ann, "key-00000012", order(4, "")).andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(created.replaceAll(".*?\"id\":(\\d+).*", "$1"));
        mvc.perform(get("/api/orders").with(as(ann))).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].id").value(id));
        mvc.perform(get("/api/orders").with(as(bob))).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/orders/" + id).with(as(ann))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(get("/api/orders/" + id).with(as(bob))).andExpect(status().isNotFound());
        mvc.perform(post("/api/orders/" + id + "/cancel").with(as(bob))).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders").param("status", "SHIPPED").param("size", "5000").with(as(ann)))
                .andExpect(jsonPath("$.totalElements").value(0)).andExpect(jsonPath("$.size").value(25));

        mvc.perform(post("/api/orders/" + id + "/cancel").with(as(ann))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(stock()).isEqualTo(10);
        mvc.perform(post("/api/orders/" + id + "/cancel").with(as(ann))).andExpect(status().isConflict());
    }

    @Test
    void managersListAndMoveEveryOrderButCustomersCannot() throws Exception {
        String created = place(ann, "key-00000013", order(1, "")).andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(created.replaceAll(".*?\"id\":(\\d+).*", "$1"));

        mvc.perform(get("/api/manage/orders").with(as(ann))).andExpect(status().isForbidden());
        mvc.perform(post("/api/manage/orders/" + id + "/status").with(as(ann)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"SHIPPED\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/manage/orders")).andExpect(status().isUnauthorized());

        var asManager = as(manager);
        mvc.perform(get("/api/manage/orders").param("q", "ann@example.com").with(asManager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].customerEmail").value("ann@example.com"));
        mvc.perform(get("/api/manage/orders").param("q", String.valueOf(id)).param("status", "PLACED").with(asManager))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/manage/orders").param("q", "nobody").with(asManager)).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/manage/orders/" + id).with(asManager)).andExpect(jsonPath("$.customerName").value("Ann A"));

        var move = post("/api/manage/orders/" + id + "/status").with(asManager).contentType(MediaType.APPLICATION_JSON);
        mvc.perform(post("/api/manage/orders/" + id + "/status").with(asManager).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"SHIPPED\"}")).andExpect(status().isConflict());
        mvc.perform(post("/api/manage/orders/" + id + "/status").with(asManager).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PROCESSING\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PROCESSING"));
        mvc.perform(post("/api/manage/orders/" + id + "/status").with(asManager).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"BOGUS\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/manage/orders/" + id + "/status").with(asManager).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isBadRequest());
        assertThat(move).isNotNull();
    }

    @Test
    void anonymousCallersCannotOrder() throws Exception {
        mvc.perform(post("/api/orders").header("Idempotency-Key", "key-00000014").contentType(MediaType.APPLICATION_JSON)
                .content(order(1, ""))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
    }
}
