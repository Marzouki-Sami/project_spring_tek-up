package com.marzouki.shop.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marzouki.shop.catalog.Category;
import com.marzouki.shop.catalog.CategoryRepository;
import com.marzouki.shop.catalog.Subcategory;
import com.marzouki.shop.catalog.SubcategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogApiTest {

    @Autowired MockMvc mvc;
    @Autowired CategoryRepository categories;
    @Autowired SubcategoryRepository subcategories;

    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager em;

    Long subcategoryId;

    @BeforeEach
    void data() {
        Category category = new Category();
        category.setTitle("Books");
        categories.save(category);
        Subcategory sub = new Subcategory();
        sub.setTitle("Fiction");
        sub.setCategory(category);
        subcategoryId = subcategories.save(sub).getId();
        em.flush();
        em.clear();
    }

    private RequestPostProcessor as(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private String body(String sku, String price) {
        return """
                {"sku":"%s","name":"Novel","price":%s,"stock":3,"active":true,"subcategoryId":%d}
                """.formatted(sku, price, subcategoryId);
    }

    @Test
    void anyoneCanReadTheCatalog() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        mvc.perform(get("/api/categories")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Books"))
                .andExpect(jsonPath("$[0].subcategories[0].title").value("Fiction"));
    }

    @Test
    void managersCreateFindAndDeleteProducts() throws Exception {
        mvc.perform(post("/api/products").with(as("MANAGER")).contentType(MediaType.APPLICATION_JSON)
                        .content(body("N-1", "12.50")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("Books"));

        mvc.perform(get("/api/products").param("q", "novel").param("category", "-1"))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/products").param("q", "NOVEL"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("N-1"));
    }

    @Test
    void customersAndGuestsCannotChangeTheCatalog() throws Exception {
        mvc.perform(post("/api/products").with(as("CUSTOMER")).contentType(MediaType.APPLICATION_JSON)
                .content(body("N-2", "5"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content(body("N-3", "5"))).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/products/1").with(as("CUSTOMER"))).andExpect(status().isForbidden());
    }

    @Test
    void invalidProductsAndDuplicatesAreRejected() throws Exception {
        mvc.perform(post("/api/products").with(as("MANAGER")).contentType(MediaType.APPLICATION_JSON)
                .content(body("N-4", "-1"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").exists());
        mvc.perform(post("/api/products").with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content(body("N-5", "5"))).andExpect(status().isCreated());
        mvc.perform(post("/api/products").with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content(body("n-5", "5"))).andExpect(status().isConflict());
    }

    @Test
    void unknownProductsAreNotFoundAndDocsArePublic() throws Exception {
        mvc.perform(get("/api/products/999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not found"));
        mvc.perform(get("/api/openapi")).andExpect(status().isOk()).andExpect(jsonPath("$.paths").exists());
    }
}