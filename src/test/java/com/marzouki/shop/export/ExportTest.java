package com.marzouki.shop.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marzouki.shop.audit.AuditRepository;
import com.marzouki.shop.cart.Cart;
import com.marzouki.shop.cart.CartService;
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
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ExportTest {

    @Autowired MockMvc mvc;
    @Autowired UserService users;
    @Autowired UserRepository userRepository;
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
    @Autowired CouponRepository coupons;
    @Autowired AuditRepository audit;

    User manager;
    User admin;
    User evil;
    Long subcategoryId;
    Long variantId;

    @BeforeEach
    void data() {
        audit.deleteAll();
        manager = users.create("Mo", "Manager", "mo@example.com", TestPasswords.strong(), Role.MANAGER);
        admin = users.create("Ada", "Admin", "ada@example.com", TestPasswords.strong(), Role.ADMIN);
        evil = users.create("=HYPERLINK(\"http://evil.test\")", "Buyer, Jr", "evil@example.com", TestPasswords.strong(), Role.CUSTOMER);
        CategoryForm category = new CategoryForm();
        category.setTitle("Home");
        SubcategoryForm sub = new SubcategoryForm();
        sub.setTitle("Lamps");
        sub.setCategoryId(categoryService.create(category).getId());
        subcategoryId = subcategoryService.create(sub).getId();
        ProductForm form = new ProductForm();
        form.setName("Lamp");
        form.setPrice(new BigDecimal("10.00"));
        form.setStock(20);
        form.setSubcategoryId(subcategoryId);
        var lamp = productService.create(form);
        variantId = variantService.productWithVariants(lamp.getId()).getDefaultVariant().getId();
        orders.createCoupon("SAVE10", 10, null, 5);

        Cart cart = new Cart();
        carts.add(cart, variantId, 2);
        ShippingForm address = new ShippingForm();
        address.setFullName(evil.getFullName());
        address.setAddress("1 Main Street");
        address.setCity("Lyon");
        address.setPostalCode("69001");
        address.setCountry("France");
        orders.place(evil.getId(), address, cart);
    }

    @AfterEach
    void cleanUp() {
        movements.deleteAll();
        orderRepository.deleteAll();
        coupons.deleteAll();
        productRepository.deleteAll();
        subcategories.deleteAll();
        categories.deleteAll();
        audit.deleteAll();
        userRepository.deleteAll();
    }

    private String download(String path, User who, String... params) throws Exception {
        var request = get(path).with(user(new AppUserDetails(who)));
        for (int i = 0; i + 1 < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        var result = mvc.perform(request).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("text/csv")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store"))).andReturn();
        return result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    void cellsAreQuotedAndFormulasAreDefused() {
        assertThat(CsvWriter.cell("plain")).isEqualTo("plain");
        assertThat(CsvWriter.cell("a,b")).isEqualTo("\"a,b\"");
        assertThat(CsvWriter.cell("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(CsvWriter.cell("line1\nline2")).isEqualTo("\"line1\nline2\"");
        for (String start : List.of("=1+1", "+1", "-1", "@SUM(A1)", "\tcmd", "\rcmd")) {
            assertThat(CsvWriter.cell(start)).as(start).matches(s -> s.startsWith("'") || s.startsWith("\"'"));
        }
        assertThat(CsvWriter.cell("=cmd|' /C calc'!A0")).isEqualTo("'=cmd|' /C calc'!A0");
        assertThat(CsvWriter.cell(null)).isEmpty();
        assertThat(CsvWriter.cell(new BigDecimal("-5.50"))).isEqualTo("-5.50");
        assertThat(CsvWriter.cell(-3)).isEqualTo("-3");
        assertThat(CsvWriter.cell(true)).isEqualTo("true");
        assertThat(CsvWriter.cell(Instant.parse("2026-01-02T03:04:05Z"))).isEqualTo("2026-01-02T03:04:05Z");
        assertThat(CsvWriter.cell(Role.ADMIN)).isEqualTo("ADMIN");
    }

    @Test
    void theFileStartsWithAByteOrderMarkAndUsesWindowsLineEnds() throws Exception {
        StringWriter out = new StringWriter();
        new CsvWriter(out).row("a", "b");
        assertThat(out.toString()).isEqualTo("\uFEFFa,b\r\n");
    }

    @Test
    void ordersExportMatchesTheListAndNeutralisesWhatCustomersTyped() throws Exception {
        String csv = download("/manage/orders/export.csv", manager);
        assertThat(csv).startsWith("\uFEFFOrder,Placed at,Status,Customer,Email,City,Country,Coupon,Subtotal,Discount,Total\r\n");
        assertThat(csv).contains("PLACED").contains("evil@example.com").contains("Lyon").contains("20.00");
        assertThat(csv).contains("\"'=HYPERLINK(\"\"http://evil.test\"\") Buyer, Jr\"");
        assertThat(csv).doesNotContain(",=HYPERLINK").doesNotContain("\n=HYPERLINK");
        assertThat(download("/manage/orders/export.csv", manager, "status", "SHIPPED").trim().split("\r\n")).hasSize(1);
        assertThat(download("/manage/orders/export.csv", manager, "q", "lyon").trim().split("\r\n")).hasSize(2);
    }

    @Test
    void customersProductsCouponsAndStockHistoryExport() throws Exception {
        assertThat(download("/manage/customers/export.csv", manager)).contains("Customer,Email,Joined,Active,Orders,Spent,Last order")
                .contains("evil@example.com").contains("20.00").doesNotContain("mo@example.com");
        assertThat(download("/manage/products/export.csv", manager)).contains("SKU,Name,Category,Subcategory,Supplier,Type,Price,Active,Variants,Stock,Low stock level")
                .contains("Lamp").contains("Home").contains("Lamps");
        assertThat(download("/manage/coupons/export.csv", manager)).contains("Code,Percent off").contains("SAVE10").contains("10,true");
        String stock = download("/manage/stock/export.csv", manager);
        assertThat(stock).contains("When,SKU,Product,Change,Reason,Note,Order,By").contains("Lamp").contains("-2").contains("Initial stock");
        assertThat(download("/manage/stock/export.csv", manager, "from", "2000-01-01", "to", "2000-01-02").trim().split("\r\n")).hasSize(1);
        assertThat(download("/manage/stock/export.csv", manager, "from", "garbage")).contains("When,SKU");
    }

    @Test
    void largeListsExportInPagesWithoutLosingRows() throws Exception {
        for (int i = 0; i < 104; i++) {
            ProductForm form = new ProductForm();
            form.setName("Bulk " + i);
            form.setPrice(BigDecimal.ONE);
            form.setSubcategoryId(subcategoryId);
            productService.create(form);
        }
        String csv = download("/manage/products/export.csv", manager);
        assertThat(csv.trim().split("\r\n")).hasSize(1 + 105);
        assertThat(csv.lines().filter(l -> l.contains("Bulk ")).count()).isEqualTo(104);
    }

    @Test
    void exportsAreRecordedInTheAuditLog() throws Exception {
        download("/manage/customers/export.csv", manager, "q", "evil");
        assertThat(audit.findAll()).anySatisfy(e -> {
            assertThat(e.getAction()).isEqualTo("export.customers");
            assertThat(e.getActorEmail()).isEqualTo("mo@example.com");
            assertThat(e.getSummary()).contains("q=evil");
        });
    }

    @Test
    void theAuditLogExportIsForAdminsOnly() throws Exception {
        mvc.perform(get("/admin/audit/export.csv").with(user(new AppUserDetails(manager)))).andExpect(status().isForbidden());
        download("/manage/customers/export.csv", manager);
        String csv = download("/admin/audit/export.csv", admin);
        assertThat(csv).startsWith("\uFEFFWhen,Who,Action,Method,Item,Item id,Result,Details,IP").contains("export.customers");
    }

    @Test
    void customersCannotExportAnything() throws Exception {
        var asCustomer = user(new AppUserDetails(evil));
        for (String path : List.of("/manage/orders/export.csv", "/manage/products/export.csv", "/manage/customers/export.csv",
                "/manage/coupons/export.csv", "/manage/stock/export.csv", "/admin/audit/export.csv")) {
            mvc.perform(get(path).with(asCustomer)).andExpect(status().isForbidden());
        }
        mvc.perform(get("/manage/orders/export.csv")).andExpect(status().is3xxRedirection());
    }
}
