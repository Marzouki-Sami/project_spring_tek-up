package com.marzouki.shop.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marzouki.shop.cart.Cart;
import com.marzouki.shop.cart.CartService;
import com.marzouki.shop.catalog.Category;
import com.marzouki.shop.catalog.CategoryRepository;
import com.marzouki.shop.catalog.Product;
import com.marzouki.shop.catalog.ProductRepository;
import com.marzouki.shop.catalog.Subcategory;
import com.marzouki.shop.catalog.SubcategoryRepository;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.NotFoundException;
import com.marzouki.shop.order.dto.ShippingForm;
import com.marzouki.shop.support.TestPasswords;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OrderServiceTest {

    @Autowired OrderService orders;
    @Autowired CartService carts;
    @Autowired UserService users;
    @Autowired ProductRepository products;
    @Autowired com.marzouki.shop.catalog.ProductVariantRepository variantRepository;
    @Autowired CategoryRepository categories;
    @Autowired SubcategoryRepository subcategories;
    @Autowired StockMovementRepository movements;
    @Autowired OrderRepository orderRepository;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager em;

    User buyer;
    Product lamp;

    @BeforeEach
    void data() {
        buyer = users.create("Bea", "Buyer", "bea@example.com", TestPasswords.strong(), Role.CUSTOMER);
        Category category = new Category();
        category.setTitle("Home");
        categories.save(category);
        Subcategory sub = new Subcategory();
        sub.setTitle("Lamps");
        sub.setCategory(category);
        subcategories.save(sub);
        lamp = product("L-1", "Lamp", "20.00", 5, sub);
    }

    private Product product(String sku, String name, String price, int stock, Subcategory sub) {
        Product p = new Product();
        p.setSku(sku);
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        p.addVariant(sku, stock);
        p.setSubcategory(sub);
        return products.save(p);
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

    private int stockOf(Long id) {
        em.flush();
        em.clear();
        return products.findById(id).orElseThrow().getStock();
    }

    @Test
    void placingAnOrderCopiesLinesTakesStockAndLogsTheMovement() {
        Cart cart = new Cart();
        carts.add(cart, lamp.getDefaultVariant().getId(), 2);
        Order order = orders.place(buyer.getId(), address(), cart);

        assertThat(order.getTotal()).isEqualByComparingTo("40.00");
        assertThat(order.getItems()).singleElement().satisfies(i -> {
            assertThat(i.getName()).isEqualTo("Lamp");
            assertThat(i.getQuantity()).isEqualTo(2);
        });
        assertThat(stockOf(lamp.getId())).isEqualTo(3);
        assertThat(movements.findByOrderIdOrderByIdAsc(order.getId())).singleElement()
                .satisfies(m -> assertThat(m.getDelta()).isEqualTo(-2));
    }

    @Test
    void theBasketCannotExceedTheStock() {
        Cart cart = new Cart();
        carts.add(cart, lamp.getDefaultVariant().getId(), 50);
        assertThat(cart.quantityOf(lamp.getDefaultVariant().getId())).isEqualTo(5);
    }

    @Test
    void stockThatDisappearsAfterAddingShrinksTheBasketLine() {
        Cart cart = new Cart();
        carts.add(cart, lamp.getDefaultVariant().getId(), 3);
        variantRepository.takeStock(lamp.getDefaultVariant().getId(), 4);
        em.clear();

        var line = carts.view(cart).lines().get(0);
        assertThat(line.quantity()).isEqualTo(1);
        assertThat(line.limited()).isTrue();
    }

    @Test
    void anEmptyBasketCannotBeOrdered() {
        assertThatThrownBy(() -> orders.place(buyer.getId(), address(), new Cart()))
                .isInstanceOf(ConflictException.class).hasMessageContaining("empty");
    }

    @Test
    void aCouponReducesTheTotalAndCountsItsUse() {
        orders.createCoupon("save25", 25, null, 1);
        Cart cart = new Cart();
        carts.add(cart, lamp.getDefaultVariant().getId(), 2);
        carts.applyCoupon(cart, "SAVE25");

        Order order = orders.place(buyer.getId(), address(), cart);
        assertThat(order.getDiscount()).isEqualByComparingTo("10.00");
        assertThat(order.getTotal()).isEqualByComparingTo("30.00");

        em.flush();
        em.clear();
        Cart second = new Cart();
        carts.add(second, lamp.getDefaultVariant().getId(), 1);
        assertThatThrownBy(() -> carts.applyCoupon(second, "SAVE25")).isInstanceOf(ConflictException.class);
    }

    @Test
    void expiredAndUnknownCouponsAreRefused() {
        orders.createCoupon("OLD", 10, Instant.now().minusSeconds(60), null);
        Cart cart = new Cart();
        assertThatThrownBy(() -> carts.applyCoupon(cart, "OLD")).hasMessageContaining("expired");
        assertThatThrownBy(() -> carts.applyCoupon(cart, "NOPE")).hasMessageContaining("not valid");
    }

    @Test
    void cancellingReturnsStockAndTheCouponUse() {
        orders.createCoupon("ONCE", 10, null, 1);
        Cart cart = new Cart();
        carts.add(cart, lamp.getDefaultVariant().getId(), 2);
        carts.applyCoupon(cart, "ONCE");
        Order order = orders.place(buyer.getId(), address(), cart);

        orders.cancelOwn(order.getId(), buyer.getId());

        assertThat(stockOf(lamp.getId())).isEqualTo(5);
        Cart again = new Cart();
        carts.add(again, lamp.getDefaultVariant().getId(), 1);
        carts.applyCoupon(again, "ONCE");
        assertThat(orders.get(order.getId()).getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void statusMovesForwardOnlyAndOwnersCannotCancelOnceProcessing() {
        Cart cart = new Cart();
        carts.add(cart, lamp.getDefaultVariant().getId(), 1);
        Order order = orders.place(buyer.getId(), address(), cart);

        assertThatThrownBy(() -> orders.changeStatus(order.getId(), OrderStatus.DELIVERED))
                .isInstanceOf(ConflictException.class);
        orders.changeStatus(order.getId(), OrderStatus.PROCESSING);
        assertThatThrownBy(() -> orders.cancelOwn(order.getId(), buyer.getId()))
                .isInstanceOf(ConflictException.class);
        orders.changeStatus(order.getId(), OrderStatus.SHIPPED);
        orders.changeStatus(order.getId(), OrderStatus.DELIVERED);
        assertThatThrownBy(() -> orders.changeStatus(order.getId(), OrderStatus.CANCELLED))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void customersCannotSeeOtherCustomersOrders() {
        Cart cart = new Cart();
        carts.add(cart, lamp.getDefaultVariant().getId(), 1);
        Order order = orders.place(buyer.getId(), address(), cart);
        User other = users.create("Oz", "Other", "oz@example.com", TestPasswords.strong(), Role.CUSTOMER);

        assertThatThrownBy(() -> orders.getOwned(order.getId(), other.getId())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void anOrderIsRefusedWhenTheTotalChangedSinceTheCustomerSawIt() {
        Cart cart = new Cart();
        carts.add(cart, lamp.getDefaultVariant().getId(), 2);
        ShippingForm form = address();
        form.setExpectedTotal(new BigDecimal("39.00"));
        assertThatThrownBy(() -> orders.place(buyer.getId(), form, cart))
                .isInstanceOf(ConflictException.class).hasMessageContaining("changed");
        assertThat(stockOf(lamp.getId())).isEqualTo(5);

        form.setExpectedTotal(new BigDecimal("40.00"));
        assertThat(orders.place(buyer.getId(), form, cart).getTotal()).isEqualByComparingTo("40.00");
    }

    @Test
    void aStatusChangeThatLostARaceIsRefusedAndRestocksOnlyOnce() {
        Cart cart = new Cart();
        carts.add(cart, lamp.getDefaultVariant().getId(), 2);
        Order order = orders.place(buyer.getId(), address(), cart);

        // Someone else cancels first; this caller still sees the order as placed.
        orderRepository.transition(order.getId(), OrderStatus.PLACED, OrderStatus.CANCELLED);

        assertThatThrownBy(() -> orders.cancelOwn(order.getId(), buyer.getId()))
                .isInstanceOf(ConflictException.class).hasMessageContaining("just changed");
        assertThat(stockOf(lamp.getId())).isEqualTo(3);
    }
}