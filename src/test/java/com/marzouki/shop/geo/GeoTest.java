package com.marzouki.shop.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marzouki.shop.security.AppUserDetails;
import com.marzouki.shop.support.TestPasswords;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserRepository;
import com.marzouki.shop.user.UserService;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(GeoTest.Fake.class)
class GeoTest {

    static final AtomicInteger CALLS = new AtomicInteger();

    @TestConfiguration
    static class Fake {
        @Bean
        @Primary
        GeocodingClient fakeGeocoder() {
            return new GeocodingClient() {
                @Override
                public List<GeoPlace> search(String query) {
                    CALLS.incrementAndGet();
                    return List.of(new GeoPlace("1 Rue Test, 69001 Lyon, France", "1 Rue Test", "69001", "Lyon", "France", 45.764, 4.8357));
                }

                @Override
                public Optional<GeoPlace> reverse(double lat, double lon) {
                    CALLS.incrementAndGet();
                    return lat > 80 ? Optional.empty()
                            : Optional.of(new GeoPlace("2 Rue Test, Lyon", "2 Rue Test", "69002", "Lyon", "France", lat, lon));
                }
            };
        }
    }

    @Autowired MockMvc mvc;
    @Autowired UserService users;
    @Autowired UserRepository userRepository;

    User cy;

    @BeforeEach
    void data() {
        CALLS.set(0);
        cy = users.create("Cy", "Customer", "cy@example.com", TestPasswords.strong(), Role.CUSTOMER);
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void addressSearchNeedsSignIn() throws Exception {
        mvc.perform(get("/geo/search").param("q", "rue test")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/geo/reverse").param("lat", "45").param("lon", "4")).andExpect(status().is3xxRedirection());
    }

    @Test
    void searchReturnsPlacesAndIdenticalSearchesAreServedFromTheCache() throws Exception {
        var asCy = user(new AppUserDetails(cy));
        mvc.perform(get("/geo/search").param("q", "Rue Test, Lyon").with(asCy)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("Lyon")).andExpect(jsonPath("$[0].lat").value(45.764));
        mvc.perform(get("/geo/search").param("q", "  rue   test, lyon ").with(asCy)).andExpect(status().isOk());
        assertThat(CALLS.get()).isEqualTo(1);
    }

    @Test
    void badInputIsRefusedBeforeReachingTheProvider() throws Exception {
        var asCy = user(new AppUserDetails(cy));
        mvc.perform(get("/geo/search").param("q", "ab").with(asCy)).andExpect(status().isBadRequest());
        mvc.perform(get("/geo/search").param("q", "x".repeat(201)).with(asCy)).andExpect(status().isBadRequest());
        mvc.perform(get("/geo/reverse").param("lat", "95").param("lon", "4").with(asCy)).andExpect(status().isBadRequest());
        mvc.perform(get("/geo/reverse").param("lat", "45").param("lon", "181").with(asCy)).andExpect(status().isBadRequest());
        assertThat(CALLS.get()).isZero();
    }

    @Test
    void reverseLookupReturnsTheAddressOrANotFoundMessage() throws Exception {
        var asCy = user(new AppUserDetails(cy));
        mvc.perform(get("/geo/reverse").param("lat", "45.76").param("lon", "4.83").with(asCy)).andExpect(status().isOk())
                .andExpect(jsonPath("$.street").value("2 Rue Test"));
        mvc.perform(get("/geo/reverse").param("lat", "85").param("lon", "4.83").with(asCy)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void theAddressFormOffersTheMapWhenEnabled() throws Exception {
        mvc.perform(get("/account/addresses/new").with(user(new AppUserDetails(cy)))).andExpect(status().isOk())
                .andExpect(content().string(containsString("data-geo=\"true\"")))
                .andExpect(content().string(containsString("geo-map")))
                .andExpect(content().string(containsString("/webjars/leaflet/")));
    }
}