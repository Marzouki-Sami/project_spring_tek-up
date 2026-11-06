package com.marzouki.shop.setup;

import com.marzouki.shop.catalog.Category;
import com.marzouki.shop.catalog.CategoryRepository;
import com.marzouki.shop.catalog.Product;
import com.marzouki.shop.catalog.ProductRepository;
import com.marzouki.shop.catalog.Subcategory;
import com.marzouki.shop.catalog.SubcategoryRepository;
import com.marzouki.shop.catalog.Supplier;
import com.marzouki.shop.catalog.SupplierRepository;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.UserRepository;
import com.marzouki.shop.user.UserService;
import java.math.BigDecimal;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills an empty database with a believable shop, for demos and screenshots. Only runs with the
 * {@code demo} profile. Account passwords are random and printed once to the log.
 */
@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataLoader implements ApplicationRunner {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserService users;
    private final UserRepository userRepository;
    private final CategoryRepository categories;
    private final SubcategoryRepository subcategories;
    private final SupplierRepository suppliers;
    private final ProductRepository products;
    private final com.marzouki.shop.order.OrderService orders;
    private final com.marzouki.shop.cart.CartService carts;
    private final com.marzouki.shop.engagement.EngagementService engagement;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0 || products.count() > 0) {
            return;
        }
        createAccount("Ada", "Admin", "admin@demo.shop", Role.ADMIN);
        createAccount("Max", "Manager", "manager@demo.shop", Role.MANAGER);
        createAccount("Cleo", "Customer", "customer@demo.shop", Role.CUSTOMER);

        Supplier nordic = supplier("Nordic Goods AB", "Elin Berg", "orders@nordic-goods.example");
        Supplier atelier = supplier("Atelier Lumi", "Jules Martin", "hello@atelier-lumi.example");

        Subcategory laptops = sub("Computers", "Laptops");
        Subcategory audio = sub("Electronics", "Audio");
        Subcategory wearables = sub("Electronics", "Wearables");
        Subcategory lighting = sub("Home", "Lighting");
        Subcategory kitchen = sub("Home", "Kitchen");
        Subcategory bags = sub("Fashion", "Bags");
        Subcategory shoes = sub("Fashion", "Shoes");

        product("LAP-001", "Aero 14 Ultrabook", "Light 14-inch laptop with a bright display and all-day battery.", "1199.00", 12, laptops, nordic);
        product("LAP-002", "Forge 16 Workstation", "Powerful 16-inch machine for developers and creators.", "1899.00", 5, laptops, nordic);
        product("AUD-001", "Halo Wireless Headphones", "Over-ear headphones with active noise cancelling.", "249.90", 30, audio, nordic);
        product("AUD-002", "Pebble Bluetooth Speaker", "Pocket-sized speaker with 12 hours of playtime.", "59.00", 48, audio, nordic);
        product("WEA-001", "Pulse Fitness Watch", "Tracks sleep, heart rate and workouts.", "179.00", 22, wearables, nordic);
        product("WEA-002", "Orbit Smart Ring", "Discreet health tracking for everyday wear.", "299.00", 0, wearables, nordic);
        product("LGT-001", "Lumi Desk Lamp", "Dimmable LED lamp with a warm glow.", "49.50", 40, lighting, atelier);
        product("LGT-002", "Arc Floor Lamp", "Tall brass-finish lamp for reading corners.", "139.00", 9, lighting, atelier);
        product("KIT-001", "Stoneware Mug Set", "Four hand-glazed mugs.", "42.00", 25, kitchen, atelier);
        product("KIT-002", "Cast Iron Skillet", "Pre-seasoned 26 cm skillet that lasts a lifetime.", "64.00", 14, kitchen, atelier);
        product("BAG-001", "Voyager Backpack", "Water-resistant 24 L backpack with a laptop sleeve.", "89.00", 33, bags, atelier);
        product("BAG-002", "Harbor Tote", "Waxed canvas tote for daily errands.", "54.00", 18, bags, atelier);
        product("SHO-001", "Trail Runner Sneakers", "Grippy, breathable sneakers for city and trail.", "119.00", 27, shoes, atelier);
        product("SHO-002", "Classic Leather Boots", "Handmade boots in full-grain leather.", "199.00", 4, shoes, atelier);
        orders.createCoupon("WELCOME10", 10, null, null);
        seedActivity();
        log.info("Demo data created. Sign in with admin@demo.shop, manager@demo.shop or customer@demo.shop using the passwords printed to the console above. Coupon WELCOME10 gives 10% off.");
    }

    /** A few customers with orders and reviews, so the dashboard and product pages are not empty. */
    private void seedActivity() {
        var cleo = userRepository.findByEmailIgnoreCase("customer@demo.shop").orElseThrow();
        var noor = users.create("Noor", "Haddad", "noor@demo.shop", randomPassword(), Role.CUSTOMER);
        var lena = users.create("Lena", "Fischer", "lena@demo.shop", randomPassword(), Role.CUSTOMER);

        place(cleo, "AUD-001", 1, com.marzouki.shop.order.OrderStatus.DELIVERED);
        place(cleo, "LGT-001", 2, com.marzouki.shop.order.OrderStatus.SHIPPED);
        place(noor, "AUD-001", 1, com.marzouki.shop.order.OrderStatus.DELIVERED);
        place(noor, "BAG-001", 1, com.marzouki.shop.order.OrderStatus.PROCESSING);
        place(lena, "LAP-001", 1, com.marzouki.shop.order.OrderStatus.PLACED);
        place(lena, "AUD-001", 1, com.marzouki.shop.order.OrderStatus.DELIVERED);

        review(cleo, "AUD-001", 5, "Great sound and the noise cancelling really works. Battery lasts for days.");
        review(noor, "AUD-001", 4, "Comfortable for long calls. The case feels a little plasticky.");
        review(lena, "AUD-001", 5, null);
        review(cleo, "LGT-001", 5, "Warm light, easy to dim.");
    }

    private void place(com.marzouki.shop.user.User buyer, String sku, int quantity,
                       com.marzouki.shop.order.OrderStatus target) {
        var cart = new com.marzouki.shop.cart.Cart();
        carts.add(cart, products.findAll().stream().filter(p -> p.getSku().equals(sku)).findFirst().orElseThrow().getId(),
                quantity);
        var address = new com.marzouki.shop.order.dto.ShippingForm();
        address.setFullName(buyer.getFullName());
        address.setAddress("12 Example Street");
        address.setCity("Lyon");
        address.setPostalCode("69001");
        address.setCountry("France");
        var order = orders.place(buyer.getId(), address, cart);
        var path = java.util.List.of(com.marzouki.shop.order.OrderStatus.PROCESSING,
                com.marzouki.shop.order.OrderStatus.SHIPPED, com.marzouki.shop.order.OrderStatus.DELIVERED);
        for (var step : path) {
            if (order.getStatus() == target) {
                break;
            }
            orders.changeStatus(order.getId(), step);
            order.setStatus(step);
        }
    }

    private void review(com.marzouki.shop.user.User author, String sku, int rating, String comment) {
        var form = new com.marzouki.shop.engagement.dto.ReviewForm();
        form.setRating(rating);
        form.setComment(comment);
        engagement.submit(author.getId(), products.findAll().stream().filter(p -> p.getSku().equals(sku)).findFirst().orElseThrow().getId(), form);
    }

    private void createAccount(String first, String last, String email, Role role) {
        String generated = randomPassword();
        users.create(first, last, email, generated, role);
        // Printed to the console, not the log: it is a one-off credential for a local demo database.
        System.out.printf("Demo account %s (%s): %s%n", email, role, generated);
    }

    /** Meets the password policy: one character from each class plus random filler. */
    private static String randomPassword() {
        String[] pools = {"ABCDEFGHJKLMNPQRSTUVWXYZ", "abcdefghijkmnopqrstuvwxyz", "23456789", "!@#$%^&*-_+="};
        StringBuilder out = new StringBuilder();
        for (String pool : pools) {
            out.append(pool.charAt(RANDOM.nextInt(pool.length())));
        }
        String all = String.join("", pools);
        for (int i = 0; i < 12; i++) {
            out.append(all.charAt(RANDOM.nextInt(all.length())));
        }
        return out.toString();
    }

    private Supplier supplier(String company, String contact, String email) {
        Supplier supplier = new Supplier();
        supplier.setCompany(company);
        supplier.setContactName(contact);
        supplier.setEmail(email);
        return suppliers.save(supplier);
    }

    private Subcategory sub(String categoryTitle, String title) {
        Category category = categories.findAll().stream()
                .filter(c -> c.getTitle().equals(categoryTitle)).findFirst()
                .orElseGet(() -> {
                    Category created = new Category();
                    created.setTitle(categoryTitle);
                    return categories.save(created);
                });
        return subcategories.findAll().stream()
                .filter(s -> s.getTitle().equals(title) && s.getCategory().getId().equals(category.getId()))
                .findFirst()
                .orElseGet(() -> {
                    Subcategory created = new Subcategory();
                    created.setTitle(title);
                    created.setCategory(category);
                    return subcategories.save(created);
                });
    }

    private void product(String sku, String name, String description, String price, int stock,
                         Subcategory subcategory, Supplier supplier) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setSubcategory(subcategory);
        product.setSupplier(supplier);
        products.save(product);
    }
}