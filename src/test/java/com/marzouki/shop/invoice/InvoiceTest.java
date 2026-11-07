package com.marzouki.shop.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.mail.Email;
import com.marzouki.shop.mail.MailOutbox;
import com.marzouki.shop.order.Order;
import com.marzouki.shop.order.OrderRepository;
import com.marzouki.shop.order.OrderService;
import com.marzouki.shop.order.OrderStatus;
import com.marzouki.shop.order.StockMovementRepository;
import com.marzouki.shop.order.dto.ShippingForm;
import com.marzouki.shop.security.AppUserDetails;
import com.marzouki.shop.settings.SettingsForm;
import com.marzouki.shop.settings.SettingsService;
import com.marzouki.shop.support.TestPasswords;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserRepository;
import com.marzouki.shop.user.UserService;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(InvoiceTest.Recording.class)
class InvoiceTest {

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
    @Autowired InvoiceService invoices;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired SettingsService settings;
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

    User ann;
    User bob;
    User manager;
    Long variantId;

    @BeforeEach
    void data() {
        SENT.clear();
        ann = users.create("Zoë", "Ñandú <b>&amp;</b>", "ann@example.com", TestPasswords.strong(), Role.CUSTOMER);
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
        form.setStock(100);
        form.setSubcategoryId(subcategoryService.create(sub).getId());
        var lamp = productService.create(form);
        variantId = variantService.productWithVariants(lamp.getId()).getDefaultVariant().getId();
    }

    @AfterEach
    void cleanUp() {
        SettingsForm form = settings.form();
        form.setShopName("Shop");
        form.setVatPercent(new BigDecimal("20.0"));
        form.setCompanyAddress(null);
        form.setVatNumber(null);
        settings.save(form);
        movements.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        subcategories.deleteAll();
        categories.deleteAll();
        userRepository.deleteAll();
    }

    private Order order(User buyer, int quantity) {
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

    private String textOf(byte[] pdf) throws IOException {
        try (PDDocument document = PDDocument.load(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    @Test
    void vatIsTheIncludedShareOfTheTotal() {
        InvoiceAmounts amounts = InvoiceAmounts.of(new BigDecimal("59.90"), new BigDecimal("20.0"));
        assertThat(amounts.net()).isEqualByComparingTo("49.92");
        assertThat(amounts.vat()).isEqualByComparingTo("9.98");
        assertThat(amounts.net().add(amounts.vat())).isEqualByComparingTo("59.90");
        assertThat(InvoiceAmounts.of(new BigDecimal("10.00"), BigDecimal.ZERO).vat()).isEqualByComparingTo("0.00");
    }

    @Test
    void everyOrderGetsOneInvoiceWithTheNextNumber() {
        Order first = order(ann, 1);
        Order second = order(bob, 1);
        int year = Year.now(ZoneOffset.UTC).getValue();
        assertThat(invoices.issue(first.getId()).getNumber()).isEqualTo("INV-%d-000001".formatted(year));
        assertThat(invoices.issue(first.getId()).getNumber()).isEqualTo("INV-%d-000001".formatted(year));
        assertThat(invoices.issue(second.getId()).getNumber()).isEqualTo("INV-%d-000002".formatted(year));
        assertThat(invoiceRepository.count()).isEqualTo(2);
    }

    @Test
    void numbersHaveNoRepeatsAndNoGapsWhenOrdersArriveTogether() throws Exception {
        int n = 8;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<Long>> placed = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                User buyer = i % 2 == 0 ? ann : bob;
                placed.add(pool.submit(() -> {
                    go.await();
                    return order(buyer, 1).getId();
                }));
            }
            go.countDown();
            for (Future<Long> f : placed) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
        }
        Set<Integer> sequences = invoiceRepository.findAll().stream().map(Invoice::getSequence).collect(Collectors.toSet());
        assertThat(sequences).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8);
        assertThat(orderRepository.count()).isEqualTo(n);
    }

    @Test
    void theConfirmationEmailCarriesTheInvoice() throws Exception {
        order(ann, 2);
        Email mail = SENT.stream().filter(m -> m.subject().contains("confirmed")).findFirst().orElseThrow();
        assertThat(mail.attachments()).singleElement().satisfies(file -> {
            assertThat(file.filename()).startsWith("INV-").endsWith(".pdf");
            assertThat(file.contentType()).isEqualTo("application/pdf");
            assertThat(new String(file.data(), 0, 5)).isEqualTo("%PDF-");
            try {
                assertThat(textOf(file.data())).contains("20.00").contains("Lamp");
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Test
    void thePdfShowsTheOrderTheTotalsAndEscapesWhatPeopleTyped() throws Exception {
        SettingsForm form = settings.form();
        form.setShopName("Lumen Goods");
        form.setCompanyAddress("5 Rue Exemple\n75001 Paris");
        form.setVatNumber("FR123456789");
        settings.save(form);
        Order order = order(ann, 3);
        String text = textOf(invoices.document(order.getId()).pdf());
        assertThat(text).contains("INVOICE").contains("Lumen Goods").contains("5 Rue Exemple").contains("VAT no. FR123456789")
                .contains("Lamp").contains("30.00").contains("25.00").contains("5.00").contains("VAT 20.0% (included)")
                .contains("Zoë").contains("Ñandú").contains("<b>&amp;</b>");
    }

    @Test
    void anIssuedInvoiceKeepsItsOwnDetailsWhenSettingsChange() throws Exception {
        Order order = order(ann, 1);
        invoices.issue(order.getId());
        SettingsForm form = settings.form();
        form.setShopName("Renamed Shop");
        form.setVatPercent(new BigDecimal("5.5"));
        settings.save(form);
        String text = textOf(invoices.document(order.getId()).pdf());
        assertThat(text).contains("VAT 20.0% (included)").doesNotContain("Renamed Shop").doesNotContain("5.5%");
    }

    @Test
    void cancelledOrdersHaveNoNewInvoiceAndOldOnesAreMarked() throws Exception {
        Order sent = order(ann, 1);
        invoices.issue(sent.getId());
        orders.changeStatus(sent.getId(), OrderStatus.CANCELLED);
        assertThat(textOf(invoices.document(sent.getId()).pdf())).contains("ORDER CANCELLED");

        Order never = order(bob, 1);
        invoiceRepository.deleteAll();
        orders.changeStatus(never.getId(), OrderStatus.CANCELLED);
        assertThatThrownBy(() -> invoices.issue(never.getId())).isInstanceOf(ConflictException.class);
    }

    @Test
    void customersDownloadTheirOwnInvoiceAndManagersAnyOne() throws Exception {
        Order order = order(ann, 1);
        var asAnn = user(new AppUserDetails(ann));
        mvc.perform(get("/account/orders/" + order.getId() + "/invoice.pdf").with(asAnn)).andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment; filename=\"INV-")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
        mvc.perform(get("/account/orders/" + order.getId() + "/invoice.pdf").with(user(new AppUserDetails(bob)))).andExpect(status().isNotFound());
        mvc.perform(get("/manage/orders/" + order.getId() + "/invoice.pdf").with(asAnn)).andExpect(status().isForbidden());
        mvc.perform(get("/manage/orders/" + order.getId() + "/invoice.pdf").with(user(new AppUserDetails(manager)))).andExpect(status().isOk());
        mvc.perform(get("/account/orders/" + order.getId() + "/invoice.pdf")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/account/orders/" + order.getId()).with(asAnn)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("invoice.pdf")));
    }

    @Test
    void theApiServesInvoicesToTheSameRules() throws Exception {
        Order order = order(ann, 1);
        var asAnn = jwt().jwt(j -> j.subject(String.valueOf(ann.getId()))).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        var asBob = jwt().jwt(j -> j.subject(String.valueOf(bob.getId()))).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        var asManager = jwt().jwt(j -> j.subject(String.valueOf(manager.getId()))).authorities(new SimpleGrantedAuthority("ROLE_MANAGER"));
        mvc.perform(get("/api/orders/" + order.getId() + "/invoice").with(asAnn)).andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
        mvc.perform(get("/api/orders/" + order.getId() + "/invoice").with(asBob)).andExpect(status().isNotFound());
        mvc.perform(get("/api/manage/orders/" + order.getId() + "/invoice").with(asAnn)).andExpect(status().isForbidden());
        mvc.perform(get("/api/manage/orders/" + order.getId() + "/invoice").with(asManager)).andExpect(status().isOk());
    }
}
