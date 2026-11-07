package com.marzouki.shop.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marzouki.shop.catalog.dto.CategoryForm;
import com.marzouki.shop.catalog.dto.ProductForm;
import com.marzouki.shop.catalog.dto.SubcategoryForm;
import com.marzouki.shop.common.ListQuery;
import com.marzouki.shop.engagement.Review;
import com.marzouki.shop.engagement.ReviewRepository;
import com.marzouki.shop.order.StockMovementRepository;
import com.marzouki.shop.security.AppUserDetails;
import com.marzouki.shop.support.TestPasswords;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserRepository;
import com.marzouki.shop.user.UserService;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.util.List;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** The shop filters and sorts, the back-office lists, and a guard against one query per row. */
@SpringBootTest
@AutoConfigureMockMvc
class ListingTest {

    @Autowired MockMvc mvc;
    @Autowired ProductService productService;
    @Autowired CategoryService categoryService;
    @Autowired SubcategoryService subcategoryService;
    @Autowired UserService users;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired SubcategoryRepository subcategories;
    @Autowired CategoryRepository categories;
    @Autowired ReviewRepository reviews;
    @Autowired StockMovementRepository movements;
    @Autowired EntityManagerFactory emf;

    Long fashion;
    Long tops;
    Long bags;
    User manager;
    User admin;
    User customer;

    @BeforeEach
    void data() {
        CategoryForm category = new CategoryForm();
        category.setTitle("Fashion");
        fashion = categoryService.create(category).getId();
        tops = subcategory("Tops");
        bags = subcategory("Bags");
        manager = users.create("Mo", "Manager", "mo@example.com", TestPasswords.strong(), Role.MANAGER);
        admin = users.create("Ada", "Admin", "ada@example.com", TestPasswords.strong(), Role.ADMIN);
        customer = users.create("Cy", "Customer", "cy@example.com", TestPasswords.strong(), Role.CUSTOMER);
        User fan = users.create("Fay", "Fan", "fan@example.com", TestPasswords.strong(), Role.CUSTOMER);

        Product a = product("A-TEE", "Alpha tee", "10.00", 5, tops, true);
        Product b = product("B-BAG", "Beta bag", "20.00", 0, bags, true);
        product("C-CAP", "Gamma cap", "30.00", 7, tops, true);
        product("D-OLD", "Delta hidden", "15.00", 9, tops, false);
        review(a, customer, 5);
        review(a, fan, 4);
        review(b, customer, 3);
    }

    @AfterEach
    void cleanUp() {
        reviews.deleteAll();
        movements.deleteAll();
        productRepository.deleteAll();
        subcategories.deleteAll();
        categories.deleteAll();
        userRepository.deleteAll();
    }

    private Long subcategory(String title) {
        SubcategoryForm form = new SubcategoryForm();
        form.setTitle(title);
        form.setCategoryId(fashion);
        return subcategoryService.create(form).getId();
    }

    private Product product(String sku, String name, String price, int stock, Long subcategoryId, boolean active) {
        ProductForm form = new ProductForm();
        form.setSku(sku);
        form.setName(name);
        form.setPrice(new BigDecimal(price));
        form.setStock(stock);
        form.setSubcategoryId(subcategoryId);
        form.setActive(active);
        return productService.create(form);
    }

    private void review(Product product, User who, int rating) {
        Review review = new Review();
        review.setProduct(product);
        review.setUser(who);
        review.setRating(rating);
        reviews.save(review);
    }

    private List<String> names(ShopQuery query) {
        return productService.shop(query).map(Product::getName).getContent();
    }

    private ShopQuery shop(String sort) {
        ShopQuery query = new ShopQuery();
        query.setSort(sort);
        return query;
    }

    @Test
    void theShopShowsOnlyActiveProductsAndSortsThem() {
        assertThat(names(shop("name"))).containsExactly("Alpha tee", "Beta bag", "Gamma cap");
        assertThat(names(shop("price-desc"))).containsExactly("Gamma cap", "Beta bag", "Alpha tee");
        assertThat(names(shop("price-asc"))).containsExactly("Alpha tee", "Beta bag", "Gamma cap");
        assertThat(names(shop("rating"))).containsExactly("Alpha tee", "Beta bag", "Gamma cap");
        assertThat(names(shop("newest"))).containsExactly("Gamma cap", "Beta bag", "Alpha tee");
        assertThat(names(shop("nonsense"))).containsExactly("Alpha tee", "Beta bag", "Gamma cap");
    }

    @Test
    void filtersCombine() {
        ShopQuery priced = shop("name");
        priced.setMinPrice("15");
        priced.setMaxPrice("25,00");
        assertThat(names(priced)).containsExactly("Beta bag");

        ShopQuery inStock = shop("name");
        inStock.setInStock(true);
        assertThat(names(inStock)).containsExactly("Alpha tee", "Gamma cap");

        ShopQuery rated = shop("name");
        rated.setMinRating(4);
        assertThat(names(rated)).containsExactly("Alpha tee");
        rated.setMinRating(3);
        assertThat(names(rated)).containsExactly("Alpha tee", "Beta bag");

        ShopQuery inSubcategory = shop("name");
        inSubcategory.setCategory(fashion);
        inSubcategory.setSubcategory(bags);
        assertThat(names(inSubcategory)).containsExactly("Beta bag");

        ShopQuery inCategory = shop("name");
        inCategory.setCategory(fashion);
        assertThat(names(inCategory)).hasSize(3);

        ShopQuery combined = shop("price-desc");
        combined.setCategory(fashion);
        combined.setInStock(true);
        combined.setMaxPrice("20");
        assertThat(names(combined)).containsExactly("Alpha tee");

        ShopQuery searched = shop("name");
        searched.setQ("TEE");
        assertThat(names(searched)).containsExactly("Alpha tee");
        searched.setQ("%");
        assertThat(names(searched)).isEmpty();
    }

    @Test
    void badInputIsIgnoredInsteadOfFailing() {
        ShopQuery query = shop("name");
        query.setMinPrice("abc");
        query.setMaxPrice("-5");
        query.setMinRating(99);
        query.setPage(-4);
        assertThat(names(query)).hasSize(3);
        assertThat(query.isFiltered()).isFalse();
        query.setMaxPrice("1e999");
        assertThat(query.maxPriceValue()).isNull();
    }

    @Test
    void theShopPageRendersWithFiltersAndStaysClearOfNPlusOne() throws Exception {
        for (int i = 0; i < 20; i++) {
            product("BULK-" + i, "Bulk item " + i, "5.00", 3, tops, true);
        }
        mvc.perform(get("/products").param("category", String.valueOf(fashion)).param("minPrice", "1")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Clear filters")));

        SessionFactory factory = emf.unwrap(SessionFactory.class);
        factory.getStatistics().setStatisticsEnabled(true);
        try {
            factory.getStatistics().clear();
            mvc.perform(get("/products").param("size", "50")).andExpect(status().isOk())
                    .andExpect(content().string(containsString("Bulk item 1")));
            assertThat(factory.getStatistics().getPrepareStatementCount()).as("statements for a page of 12 products").isLessThanOrEqualTo(10);
        } finally {
            factory.getStatistics().setStatisticsEnabled(false);
        }
        mvc.perform(get("/products").param("sort", "rating").param("page", "1")).andExpect(status().isOk());
        mvc.perform(get("/products").param("category", "abc")).andExpect(status().is4xxClientError());
    }

    @Test
    void backOfficeListsPageSearchAndFilter() throws Exception {
        var asManager = user(new AppUserDetails(manager));
        mvc.perform(get("/manage/products").with(asManager)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Alpha tee"))).andExpect(content().string(containsString("Delta hidden")));
        mvc.perform(get("/manage/products").param("q", "b-bag").with(asManager))
                .andExpect(content().string(containsString("Beta bag"))).andExpect(content().string(not(containsString("Alpha tee"))));
        mvc.perform(get("/manage/products").param("status", "HIDDEN").with(asManager))
                .andExpect(content().string(containsString("Delta hidden"))).andExpect(content().string(not(containsString("Alpha tee"))));
        mvc.perform(get("/manage/products").param("status", "OUT").with(asManager))
                .andExpect(content().string(containsString("Beta bag"))).andExpect(content().string(not(containsString("Gamma cap"))));
        mvc.perform(get("/manage/products").param("status", "LOW").with(asManager))
                .andExpect(content().string(containsString("Alpha tee"))).andExpect(content().string(not(containsString("Gamma cap"))));
        mvc.perform(get("/manage/products").param("q", "%").with(asManager)).andExpect(content().string(containsString("No products match")));
        mvc.perform(get("/manage/products").param("size", "999").param("page", "-1").param("status", "<script>").param("sort", "zzz").with(asManager))
                .andExpect(status().isOk());

        for (String path : List.of("/manage/orders", "/manage/coupons", "/manage/suppliers", "/manage/categories",
                "/manage/subcategories", "/manage/customers")) {
            mvc.perform(get(path).param("q", "x").param("size", "10").param("page", "3").with(asManager)).andExpect(status().isOk());
        }
        mvc.perform(get("/manage/orders").param("status", "PLACED").param("sort", "total-desc").param("q", "12").with(asManager)).andExpect(status().isOk());
        mvc.perform(get("/manage/orders").param("status", "NOPE").with(asManager)).andExpect(status().isOk());
        mvc.perform(get("/manage/coupons").param("status", "USABLE").param("sort", "used").with(asManager)).andExpect(status().isOk());
        mvc.perform(get("/manage/coupons").param("status", "EXPIRED").with(asManager)).andExpect(status().isOk());
        mvc.perform(get("/manage/subcategories").param("sort", "title").param("q", "bag").with(asManager))
                .andExpect(content().string(containsString("Bags"))).andExpect(content().string(not(containsString("Tops"))));

        var asAdmin = user(new AppUserDetails(admin));
        for (String status : List.of("", "CUSTOMER", "MANAGER", "ADMIN", "NO2FA", "LOCKED", "DISABLED")) {
            mvc.perform(get("/admin/users").param("status", status).param("sort", "newest").with(asAdmin)).andExpect(status().isOk());
        }
        mvc.perform(get("/admin/users").param("q", "ada").with(asAdmin))
                .andExpect(content().string(containsString("ada@example.com"))).andExpect(content().string(not(containsString("cy@example.com"))));
    }

    @Test
    void customersSortAndPageTheirOwnOrdersAndWishlist() throws Exception {
        var asCustomer = user(new AppUserDetails(customer));
        mvc.perform(get("/account/orders").param("sort", "oldest").param("status", "PLACED").with(asCustomer)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.anyOf(containsString("You have not placed an order yet"), containsString("No orders match"))));
        for (String sort : List.of("", "oldest", "name", "price-asc", "price-desc")) {
            mvc.perform(get("/account/wishlist").param("sort", sort).param("q", "a").with(asCustomer)).andExpect(status().isOk());
        }
    }

    @Test
    void backOfficeListIsAPagedQueryNotAFullTableScanInMemory() {
        for (int i = 0; i < 30; i++) {
            product("PG-" + i, "Paged " + i, "1.00", 1, bags, true);
        }
        ListQuery query = new ListQuery();
        query.setSize(10);
        query.setPage(2);
        var page = productService.search(query);
        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalElements()).isEqualTo(34);
        assertThat(page.getTotalPages()).isEqualTo(4);
    }
}
