package com.marzouki.shop.catalog.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogPagesTest {

    @Autowired MockMvc mvc;

    @Test
    void theHomePageRedirectsToTheProductList() throws Exception {
        mvc.perform(get("/")).andExpect(redirectedUrl("/products"));
    }

    @Test
    void everyListPageRenders() throws Exception {
        for (String path : new String[] {"/products", "/categories", "/subcategories", "/suppliers"}) {
            mvc.perform(get(path)).andExpect(status().isOk());
        }
    }

    @Test
    void aBlankCategoryShowsAValidationMessage() throws Exception {
        mvc.perform(post("/categories").param("title", " "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Enter a title")));
    }

    @Test
    void creatingACategoryRedirectsToTheList() throws Exception {
        mvc.perform(post("/categories").param("title", "Books"))
                .andExpect(redirectedUrl("/categories"));
        mvc.perform(get("/categories")).andExpect(content().string(containsString("Books")));
    }

    @Test
    void aDuplicateCategoryShowsAFriendlyConflictPage() throws Exception {
        mvc.perform(post("/categories").param("title", "Books"));
        mvc.perform(post("/categories").param("title", "books"))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("already exists")));
    }

    @Test
    void anUnknownProductShowsANotFoundPage() throws Exception {
        mvc.perform(get("/products/999999/edit"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("was not found")));
    }

    @Test
    void aNegativePriceIsRefused() throws Exception {
        mvc.perform(post("/products").param("sku", "A-1").param("name", "Thing")
                        .param("price", "-1").param("stock", "1").param("subcategoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The price cannot be negative")));
    }
}