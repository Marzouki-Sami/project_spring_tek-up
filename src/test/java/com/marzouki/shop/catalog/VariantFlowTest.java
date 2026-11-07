package com.marzouki.shop.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marzouki.shop.cart.Cart;
import com.marzouki.shop.cart.CartService;
import com.marzouki.shop.catalog.dto.OptionTypeForm;
import com.marzouki.shop.catalog.dto.OptionValueForm;
import com.marzouki.shop.catalog.dto.ProductForm;
import com.marzouki.shop.catalog.dto.ProductTypeForm;
import com.marzouki.shop.catalog.dto.SubcategoryForm;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.order.Order;
import com.marzouki.shop.order.OrderRepository;
import com.marzouki.shop.order.OrderService;
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
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** Not transactional on purpose, so pages are rendered the way they are in production. */
@SpringBootTest
@AutoConfigureMockMvc
class VariantFlowTest {

    @Autowired MockMvc mvc;
    @Autowired OptionService optionService;
    @Autowired ProductTypeService typeService;
    @Autowired ProductService productService;
    @Autowired VariantService variantService;
    @Autowired StockService stockService;
    @Autowired CartService carts;
    @Autowired OrderService orders;
    @Autowired UserService users;
    @Autowired SubcategoryService subcategoryService;
    @Autowired CategoryService categoryService;
    @Autowired UserRepository userRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired StockMovementRepository movements;
    @Autowired ProductRepository productRepository;
    @Autowired ProductTypeRepository productTypes;
    @Autowired OptionTypeRepository optionTypes;
    @Autowired SubcategoryRepository subcategories;
    @Autowired CategoryRepository categories;

    OptionType size;
    OptionType color;
    ProductType clothing;
    Product tee;
    Subcategory subcategory;
    User manager;
    User buyer;

    @BeforeEach
    void data() {
        size = optionService.createType(optionForm("Size", OptionType.Display.TEXT));
        color = optionService.createType(optionForm("Color", OptionType.Display.COLOR));
        for (String label : List.of("S", "M", "L")) {
            optionService.addValue(size.getId(), value(label, null));
        }
        optionService.addValue(color.getId(), value("Black", "#111111"));
        optionService.addValue(color.getId(), value("Sand", "#d6c3a1"));
        ProductTypeForm typeForm = new ProductTypeForm();
        typeForm.setName("Clothing");
        typeForm.setOptionTypeIds(List.of(size.getId(), color.getId()));
        clothing = typeService.create(typeForm);

        var categoryForm = new com.marzouki.shop.catalog.dto.CategoryForm();
        categoryForm.setTitle("Fashion");
        Category category = categoryService.create(categoryForm);
        SubcategoryForm sub = new SubcategoryForm();
        sub.setTitle("Tops");
        sub.setCategoryId(category.getId());
        subcategory = subcategoryService.create(sub);

        ProductForm form = new ProductForm();
        form.setSku("TEE");
        form.setName("Cotton tee");
        form.setPrice(new BigDecimal("24.00"));
        form.setSubcategoryId(subcategory.getId());
        form.setProductTypeId(clothing.getId());
        tee = productService.create(form);
        manager = users.create("Mo", "Manager", "mo@example.com", TestPasswords.strong(), Role.MANAGER);
        buyer = users.create("Bea", "Buyer", "bea@example.com", TestPasswords.strong(), Role.CUSTOMER);
    }

    @AfterEach
    void cleanUp() {
        movements.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        productTypes.deleteAll();
        optionTypes.deleteAll();
        subcategories.deleteAll();
        categories.deleteAll();
        userRepository.deleteAll();
    }

    private OptionTypeForm optionForm(String name, OptionType.Display display) {
        OptionTypeForm form = new OptionTypeForm();
        form.setName(name);
        form.setDisplay(display);
        return form;
    }

    private OptionValueForm value(String label, String hex) {
        OptionValueForm form = new OptionValueForm();
        form.setLabel(label);
        form.setColorHex(hex);
        return form;
    }

    private Map<Long, Set<Long>> allValues() {
        return Map.of(size.getId(), idsOf("Size"), color.getId(), idsOf("Color"));
    }

    private Set<Long> idsOf(String typeName) {
        return optionService.findAll().stream().filter(t -> t.getName().equals(typeName))
                .flatMap(t -> t.getValues().stream()).map(OptionValue::getId).collect(java.util.stream.Collectors.toSet());
    }

    private ShippingForm address() {
        ShippingForm form = new ShippingForm();
        form.setFullName("Bea Buyer");
        form.setAddress("1 Main Street");
        form.setCity("Lyon");
        form.setPostalCode("69001");
        form.setCountry("France");
        return form;
    }

    @Test
    void generatingCreatesEveryCombinationOnceAndOnlyMissingOnesLater() {
        assertThat(variantService.generate(tee.getId(), allValues())).isEqualTo(6);
        assertThat(variantService.generate(tee.getId(), allValues())).isZero();

        Product loaded = variantService.productWithVariants(tee.getId());
        assertThat(loaded.getVariants()).extracting(ProductVariant::getSku)
                .contains("TEE-S-BLACK", "TEE-L-SAND").doesNotHaveDuplicates();
        assertThat(loaded.getVariants().get(0).getLabel()).isEqualTo("Size: S, Color: Black");

        optionService.addValue(size.getId(), value("XL", null));
        assertThat(variantService.generate(tee.getId(), allValues())).isEqualTo(2);
    }

    @Test
    void chosenValuesMustCoverEveryOptionOfTheType() {
        assertThatThrownBy(() -> variantService.generate(tee.getId(), Map.of(size.getId(), idsOf("Size"))))
                .isInstanceOf(ConflictException.class).hasMessageContaining("color");
    }

    @Test
    void optionsInUseCannotBeRemoved() {
        variantService.generate(tee.getId(), allValues());
        OptionValue small = optionService.findAll().stream().filter(t -> t.getName().equals("Size"))
                .flatMap(t -> t.getValues().stream()).filter(v -> v.getLabel().equals("S")).findFirst().orElseThrow();

        assertThatThrownBy(() -> optionService.deleteValue(small.getId())).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> optionService.deleteType(size.getId())).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> typeService.delete(clothing.getId())).isInstanceOf(ConflictException.class);
    }

    @Test
    void stockAdjustmentsAreLoggedAndCannotGoBelowZero() {
        variantService.generate(tee.getId(), allValues());
        ProductVariant variant = variantService.productWithVariants(tee.getId()).getVariants().get(0);

        stockService.adjust(variant.getId(), 10, StockMovement.Reason.SUPPLIER_DELIVERY, " first delivery ");
        stockService.adjust(variant.getId(), -3, StockMovement.Reason.DAMAGED, null);
        assertThatThrownBy(() -> stockService.adjust(variant.getId(), -8, StockMovement.Reason.RECOUNT, null))
                .isInstanceOf(ConflictException.class).hasMessageContaining("Only 7");
        assertThatThrownBy(() -> stockService.adjust(variant.getId(), 0, StockMovement.Reason.RECOUNT, null))
                .isInstanceOf(ConflictException.class);

        assertThat(variantService.getForStock(variant.getId()).getStock()).isEqualTo(7);
        assertThat(stockService.history(variant.getId(), 0).getContent()).extracting(StockMovement::getDelta)
                .containsExactly(-3, 10);
        assertThat(stockService.history(variant.getId(), 0).getContent().get(1).getNote()).isEqualTo("first delivery");
    }

    @Test
    void ordersTakeStockFromTheChosenVariantOnlyAndCancellationReturnsIt() {
        variantService.generate(tee.getId(), allValues());
        List<ProductVariant> all = variantService.productWithVariants(tee.getId()).getVariants();
        ProductVariant first = all.get(0);
        ProductVariant second = all.get(1);
        stockService.adjust(first.getId(), 5, StockMovement.Reason.SUPPLIER_DELIVERY, null);
        stockService.adjust(second.getId(), 5, StockMovement.Reason.SUPPLIER_DELIVERY, null);

        Cart cart = new Cart();
        carts.add(cart, first.getId(), 2);
        Order order = orders.place(buyer.getId(), address(), cart);

        assertThat(variantService.getForStock(first.getId()).getStock()).isEqualTo(3);
        assertThat(variantService.getForStock(second.getId()).getStock()).isEqualTo(5);
        Order saved = orders.get(order.getId());
        assertThat(saved.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getVariantLabel()).isEqualTo("Size: S, Color: Black");
            assertThat(item.getSku()).isEqualTo("TEE-S-BLACK");
        });

        orders.cancelOwn(order.getId(), buyer.getId());
        assertThat(variantService.getForStock(first.getId()).getStock()).isEqualTo(5);
        assertThatThrownBy(() -> variantService.delete(tee.getId(), first.getId())).isInstanceOf(ConflictException.class);
    }

    @Test
    void aVariantPriceOverrideIsUsedInTheCart() {
        variantService.generate(tee.getId(), allValues());
        ProductVariant variant = variantService.productWithVariants(tee.getId()).getVariants().get(0);
        var form = new com.marzouki.shop.catalog.dto.VariantForm();
        form.setSku(variant.getSku());
        form.setPrice(new BigDecimal("30.00"));
        form.setActive(true);
        variantService.update(tee.getId(), variant.getId(), form);
        stockService.adjust(variant.getId(), 5, StockMovement.Reason.SUPPLIER_DELIVERY, null);

        Cart cart = new Cart();
        carts.add(cart, variant.getId(), 2);
        assertThat(carts.view(cart).total()).isEqualByComparingTo("60.00");
    }

    @Test
    void aProductWithOrdersKeepsItsType() {
        variantService.generate(tee.getId(), allValues());
        ProductVariant variant = variantService.productWithVariants(tee.getId()).getVariants().get(0);
        stockService.adjust(variant.getId(), 5, StockMovement.Reason.SUPPLIER_DELIVERY, null);
        Cart cart = new Cart();
        carts.add(cart, variant.getId(), 1);
        orders.place(buyer.getId(), address(), cart);

        ProductForm form = productService.formFor(tee.getId());
        form.setProductTypeId(null);
        assertThatThrownBy(() -> productService.update(tee.getId(), form)).isInstanceOf(ConflictException.class)
                .hasMessageContaining("orders");
    }

    @Test
    void aSimpleProductGetsOneDefaultVariantWithItsStartingStock() {
        ProductForm form = new ProductForm();
        form.setSku("MUG");
        form.setName("Mug");
        form.setPrice(new BigDecimal("9.00"));
        form.setStock(12);
        form.setSubcategoryId(subcategory.getId());
        Product mug = productService.create(form);

        Product loaded = productService.get(mug.getId());
        assertThat(loaded.getVariants()).singleElement().satisfies(v -> {
            assertThat(v.getSku()).isEqualTo("MUG");
            assertThat(v.getStock()).isEqualTo(12);
        });
        assertThat(stockService.history(loaded.getDefaultVariant().getId(), 0).getContent())
                .singleElement().satisfies(m -> assertThat(m.getReason()).isEqualTo(StockMovement.Reason.INITIAL));
    }

    @Test
    void storefrontAndBackOfficePagesRender() throws Exception {
        variantService.generate(tee.getId(), allValues());
        ProductVariant variant = variantService.productWithVariants(tee.getId()).getVariants().get(0);
        stockService.adjust(variant.getId(), 4, StockMovement.Reason.SUPPLIER_DELIVERY, null);
        var asManager = user(new AppUserDetails(manager));

        mvc.perform(get("/products/" + tee.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("opt-group")))
                .andExpect(content().string(containsString("variant-data")))
                .andExpect(content().string(containsString("TEE-S-BLACK")));
        mvc.perform(get("/products")).andExpect(status().isOk());
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/manage/products").with(asManager)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Clothing")));
        mvc.perform(get("/manage/products/" + tee.getId() + "/edit").with(asManager)).andExpect(status().isOk());
        mvc.perform(get("/manage/products/" + tee.getId() + "/variants").with(asManager)).andExpect(status().isOk())
                .andExpect(content().string(containsString("TEE-S-BLACK")));
        mvc.perform(get("/manage/options").with(asManager)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Color")));
        mvc.perform(get("/manage/product-types").with(asManager)).andExpect(status().isOk());
        mvc.perform(get("/manage/product-types/" + clothing.getId() + "/edit").with(asManager)).andExpect(status().isOk());

        mvc.perform(post("/manage/variants/" + variant.getId() + "/stock").with(asManager).with(csrf())
                        .param("delta", "6").param("reason", "RECOUNT").param("note", "shelf count"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/manage/variants/" + variant.getId() + "/stock").with(asManager)).andExpect(status().isOk())
                .andExpect(content().string(containsString("shelf count")))
                .andExpect(content().string(containsString("Mo Manager")));

        MockHttpSessionHolder holder = new MockHttpSessionHolder();
        mvc.perform(post("/cart/add").session(holder.session).with(csrf()).param("variantId", variant.getId().toString()));
        mvc.perform(get("/cart").session(holder.session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Size: S, Color: Black")));
    }

    @Test
    void customersCannotReachTheVariantAndOptionPages() throws Exception {
        var asBuyer = user(new AppUserDetails(buyer));
        mvc.perform(get("/manage/options").with(asBuyer)).andExpect(status().isForbidden());
        mvc.perform(get("/manage/product-types").with(asBuyer)).andExpect(status().isForbidden());
        mvc.perform(post("/manage/variants/1/stock").with(asBuyer).with(csrf()).param("delta", "5").param("reason", "RECOUNT"))
                .andExpect(status().isForbidden());
    }

    static class MockHttpSessionHolder {
        final org.springframework.mock.web.MockHttpSession session = new org.springframework.mock.web.MockHttpSession();
    }
}