package com.marzouki.shop.address;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marzouki.shop.address.dto.AddressForm;
import com.marzouki.shop.cart.SavedCartRepository;
import com.marzouki.shop.catalog.Category;
import com.marzouki.shop.catalog.CategoryRepository;
import com.marzouki.shop.catalog.Product;
import com.marzouki.shop.catalog.ProductRepository;
import com.marzouki.shop.catalog.Subcategory;
import com.marzouki.shop.catalog.SubcategoryRepository;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.NotFoundException;
import com.marzouki.shop.order.Order;
import com.marzouki.shop.order.OrderRepository;
import com.marzouki.shop.order.StockMovementRepository;
import com.marzouki.shop.security.AppUserDetails;
import com.marzouki.shop.support.TestPasswords;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserRepository;
import com.marzouki.shop.user.UserService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/** Not transactional, so pages render the way they do in production. */
@SpringBootTest
@AutoConfigureMockMvc
class AddressTest {

    @Autowired MockMvc mvc;
    @Autowired AddressService addresses;
    @Autowired AddressRepository addressRepository;
    @Autowired UserService users;
    @Autowired UserRepository userRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired StockMovementRepository movements;
    @Autowired SavedCartRepository savedCarts;
    @Autowired ProductRepository products;
    @Autowired SubcategoryRepository subcategories;
    @Autowired CategoryRepository categories;

    User cy;
    User sam;
    Product lamp;

    @BeforeEach
    void data() {
        cy = users.create("Cy", "Customer", "cy@example.com", TestPasswords.strong(), Role.CUSTOMER);
        sam = users.create("Sam", "Other", "sam@example.com", TestPasswords.strong(), Role.CUSTOMER);
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
        lamp.addVariant("L-1", 10);
        lamp.setSubcategory(sub);
        products.save(lamp);
    }

    @AfterEach
    void cleanUp() {
        movements.deleteAll();
        orderRepository.deleteAll();
        savedCarts.deleteAll();
        addressRepository.deleteAll();
        products.deleteAll();
        subcategories.deleteAll();
        categories.deleteAll();
        userRepository.deleteAll();
    }

    private AddressForm form(String label, String street) {
        AddressForm form = new AddressForm();
        form.setLabel(label);
        form.setFullName("Cy Customer");
        form.setAddress(street);
        form.setPostalCode("69001");
        form.setCity("Lyon");
        form.setCountry("France");
        return form;
    }

    private Long variant() {
        return lamp.getDefaultVariant().getId();
    }

    @Test
    void theFirstAddressBecomesTheDefaultAndDefaultsCanMove() {
        Address home = addresses.create(cy.getId(), form("Home", "1 Main Street"));
        Address work = addresses.create(cy.getId(), form("Work", "2 Office Road"));
        assertThat(home.isDefaultAddress()).isTrue();
        assertThat(addresses.get(work.getId(), cy.getId()).isDefaultAddress()).isFalse();

        addresses.makeDefault(work.getId(), cy.getId());
        List<Address> list = addresses.forUser(cy.getId());
        assertThat(list).extracting(Address::getLabel).containsExactly("Work", "Home");
        assertThat(list).filteredOn(Address::isDefaultAddress).hasSize(1);

        addresses.delete(work.getId(), cy.getId());
        assertThat(addresses.forUser(cy.getId())).singleElement().satisfies(a -> assertThat(a.isDefaultAddress()).isTrue());
    }

    @Test
    void addressesBelongToTheirOwnerAndAreLimited() {
        Address home = addresses.create(cy.getId(), form("Home", "1 Main Street"));
        assertThatThrownBy(() -> addresses.get(home.getId(), sam.getId())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> addresses.delete(home.getId(), sam.getId())).isInstanceOf(NotFoundException.class);

        for (int i = 1; i < AddressService.MAX_ADDRESSES; i++) {
            addresses.create(cy.getId(), form("A" + i, i + " Street"));
        }
        assertThatThrownBy(() -> addresses.create(cy.getId(), form("Too many", "99 Street")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("at most");
    }

    @Test
    void thePinIsStoredWithSixDecimalsOrNotAtAll() {
        AddressForm withPin = form("Pinned", "1 Main Street");
        withPin.setLatitude(new BigDecimal("45.7640001"));
        withPin.setLongitude(new BigDecimal("4.8357"));
        Address pinned = addresses.create(cy.getId(), withPin);
        assertThat(pinned.hasPosition()).isTrue();
        assertThat(pinned.getLatitude()).isEqualByComparingTo("45.764000");

        AddressForm halfPin = form("Half", "2 Main Street");
        halfPin.setLatitude(new BigDecimal("45.0"));
        assertThat(addresses.create(cy.getId(), halfPin).hasPosition()).isFalse();
    }

    @Test
    void theAddressBookPagesWork() throws Exception {
        var asCy = user(new AppUserDetails(cy));
        mvc.perform(get("/account/addresses").with(asCy)).andExpect(status().isOk());
        mvc.perform(get("/account/addresses/new").with(asCy)).andExpect(status().isOk())
                .andExpect(content().string(containsString("address-picker")));

        mvc.perform(post("/account/addresses").with(asCy).with(csrf()).param("label", "Home")
                        .param("fullName", "Cy Customer").param("address", "1 Main Street").param("postalCode", "69001")
                        .param("city", "Lyon").param("country", "France").param("latitude", "45.764").param("longitude", "4.8357"))
                .andExpect(status().is3xxRedirection());
        Address saved = addresses.forUser(cy.getId()).get(0);

        mvc.perform(get("/account/addresses").with(asCy)).andExpect(content().string(containsString("1 Main Street")))
                .andExpect(content().string(containsString("Pinned on the map")));
        mvc.perform(get("/account/addresses/" + saved.getId() + "/edit").with(asCy)).andExpect(status().isOk());
        mvc.perform(post("/account/addresses").with(asCy).with(csrf()).param("label", "")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Enter the street address")));

        var asSam = user(new AppUserDetails(sam));
        mvc.perform(get("/account/addresses/" + saved.getId() + "/edit").with(asSam)).andExpect(status().isNotFound());
        mvc.perform(post("/account/addresses/" + saved.getId() + "/delete").with(asSam).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(addressRepository.count()).isEqualTo(1);
    }

    @Test
    void checkoutUsesAChosenSavedAddressAndKeepsACopyWithItsPin() throws Exception {
        AddressForm pinned = form("Home", "1 Main Street");
        pinned.setLatitude(new BigDecimal("45.764"));
        pinned.setLongitude(new BigDecimal("4.8357"));
        pinned.setPhone("+33 4 00 00 00 00");
        Address home = addresses.create(cy.getId(), pinned);
        var asCy = user(new AppUserDetails(cy));
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/cart/add").session(session).with(csrf()).param("variantId", variant().toString()));

        mvc.perform(get("/checkout").session(session).with(asCy)).andExpect(status().isOk())
                .andExpect(content().string(containsString("1 Main Street")));
        mvc.perform(post("/checkout").session(session).with(asCy).with(csrf()).param("addressId", home.getId().toString()))
                .andExpect(status().is3xxRedirection());

        Order order = orderRepository.findByUserIdOrderByCreatedAtDescIdDesc(cy.getId()).get(0);
        assertThat(order.getAddress()).isEqualTo("1 Main Street");
        assertThat(order.getPhone()).isEqualTo("+33 4 00 00 00 00");
        assertThat(order.getLatitude()).isEqualByComparingTo("45.764");

        // Later edits to the address do not rewrite history.
        AddressForm moved = addresses.formFor(home.getId(), cy.getId());
        moved.setAddress("99 Elsewhere");
        addresses.update(home.getId(), cy.getId(), moved);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getAddress()).isEqualTo("1 Main Street");
    }

    @Test
    void checkoutCanSaveANewAddressAndRefusesSomeoneElsesOrIncompleteOnes() throws Exception {
        var asCy = user(new AppUserDetails(cy));
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/cart/add").session(session).with(csrf()).param("variantId", variant().toString()));

        mvc.perform(post("/checkout").session(session).with(asCy).with(csrf()).param("fullName", " "))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Enter the street address")));

        Address samsAddress = addresses.create(sam.getId(), form("Home", "7 Private Lane"));
        mvc.perform(post("/checkout").session(session).with(asCy).with(csrf()).param("addressId", samsAddress.getId().toString()))
                .andExpect(status().isNotFound());

        mvc.perform(post("/checkout").session(session).with(asCy).with(csrf())
                        .param("fullName", "Cy Customer").param("address", "5 New Road").param("city", "Lyon")
                        .param("postalCode", "69002").param("country", "France").param("saveAddress", "true")
                        .param("addressLabel", "Holiday"))
                .andExpect(status().is3xxRedirection());
        assertThat(addresses.forUser(cy.getId())).singleElement().satisfies(a -> {
            assertThat(a.getLabel()).isEqualTo("Holiday");
            assertThat(a.getStreet()).isEqualTo("5 New Road");
        });
    }
}