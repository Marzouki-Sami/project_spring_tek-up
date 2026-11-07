package com.marzouki.shop.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marzouki.shop.catalog.Category;
import com.marzouki.shop.catalog.CategoryRepository;
import com.marzouki.shop.catalog.Product;
import com.marzouki.shop.catalog.ProductRepository;
import com.marzouki.shop.catalog.ProductService;
import com.marzouki.shop.catalog.Subcategory;
import com.marzouki.shop.catalog.SubcategoryRepository;
import com.marzouki.shop.catalog.dto.ProductForm;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.security.AppUserDetails;
import com.marzouki.shop.support.TestPasswords;
import com.marzouki.shop.user.Role;
import com.marzouki.shop.user.User;
import com.marzouki.shop.user.UserRepository;
import com.marzouki.shop.user.UserService;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Not transactional: pages are rendered after the service call, as in production. */
@SpringBootTest
@AutoConfigureMockMvc
class ProductImageTest {

    @TempDir
    static Path directory;

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) {
        registry.add("app.images.dir", () -> directory.toString());
    }

    @Autowired MockMvc mvc;
    @Autowired ProductImageService images;
    @Autowired ImageStorage storage;
    @Autowired ProductService productService;
    @Autowired ProductRepository productRepository;
    @Autowired SubcategoryRepository subcategories;
    @Autowired CategoryRepository categories;
    @Autowired UserService users;
    @Autowired UserRepository userRepository;

    Product product;
    User manager;
    User customer;

    @BeforeEach
    void data() {
        Category category = new Category();
        category.setTitle("Home");
        categories.save(category);
        Subcategory sub = new Subcategory();
        sub.setTitle("Lamps");
        sub.setCategory(category);
        subcategories.save(sub);
        ProductForm form = new ProductForm();
        form.setSku("LAMP");
        form.setName("Desk lamp");
        form.setPrice(new BigDecimal("20.00"));
        form.setStock(3);
        form.setSubcategoryId(sub.getId());
        product = productService.create(form);
        manager = users.create("Mo", "Manager", "mo@example.com", TestPasswords.strong(), Role.MANAGER);
        customer = users.create("Cy", "Customer", "cy@example.com", TestPasswords.strong(), Role.CUSTOMER);
    }

    @AfterEach
    void cleanUp() {
        productRepository.deleteAll();
        subcategories.deleteAll();
        categories.deleteAll();
        userRepository.deleteAll();
    }

    private byte[] png(int width, int height) throws IOException {
        return ImageProcessorTest.image(width, height, "png", BufferedImage.TYPE_INT_RGB);
    }

    private Product reloaded() {
        return images.productWithImages(product.getId());
    }

    @Test
    void managersUploadImagesAndCustomersSeeAGalleryWithThePreviewOnCards() throws Exception {
        var asManager = user(new AppUserDetails(manager));
        mvc.perform(multipart("/manage/products/" + product.getId() + "/images")
                        .file(new MockMultipartFile("files", "a.png", "image/png", png(1600, 900)))
                        .file(new MockMultipartFile("files", "b.png", "image/png", png(800, 800)))
                        .with(asManager).with(csrf()))
                .andExpect(status().is3xxRedirection());

        List<ProductImage> stored = reloaded().getImages();
        assertThat(stored).hasSize(2).extracting(ProductImage::getPosition).containsExactly(0, 1);
        assertThat(storage.find(stored.get(0).getFileKey())).isPresent();

        mvc.perform(get("/products/" + product.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"gallery\"")))
                .andExpect(content().string(containsString(stored.get(1).getUrl())))
                .andExpect(content().string(containsString("data-bs-slide-to=\"1\"")));
        mvc.perform(get("/products")).andExpect(status().isOk())
                .andExpect(content().string(containsString(stored.get(0).getThumbUrl())));
        mvc.perform(get("/manage/products/" + product.getId() + "/images").with(asManager)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Preview")));
    }

    @Test
    void imagesAreServedWithLongCachingAndOnlyForRealKeys() throws Exception {
        ProductImage image = images.add(product.getId(), png(900, 600), "A lamp");
        mvc.perform(get(image.getUrl())).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Cache-Control", containsString("immutable")));
        mvc.perform(get("/media/" + "0".repeat(32) + ".jpg")).andExpect(status().isNotFound());
        mvc.perform(get("/media/..%2f..%2fapplication.properties")).andExpect(status().is4xxClientError());
        mvc.perform(get("/media/%2e%2e/pom.xml")).andExpect(status().is4xxClientError());
        mvc.perform(get("/media/" + image.getFileKey().replace(".jpg", ".png"))).andExpect(status().is4xxClientError());
    }

    @Test
    void imagesCanBeReorderedAndAnyOneCanBecomeThePreview() throws Exception {
        images.add(product.getId(), png(400, 300), null);
        images.add(product.getId(), png(401, 300), null);
        images.add(product.getId(), png(402, 300), null);
        Long third = reloaded().getImages().get(2).getId();

        images.makePreview(product.getId(), third);
        assertThat(reloaded().getImages()).extracting(ProductImage::getWidth).containsExactly(402, 400, 401);
        assertThat(reloaded().getPrimaryImage().getPosition()).isZero();

        images.move(product.getId(), third, 1);
        assertThat(reloaded().getImages()).extracting(ProductImage::getWidth).containsExactly(400, 402, 401);
        images.move(product.getId(), third, 99);
        assertThat(reloaded().getImages()).extracting(ProductImage::getWidth).containsExactly(400, 401, 402);
    }

    @Test
    void deletingAnImageRemovesItsFilesAndAProductHoldsAtMostEight() throws Exception {
        for (int i = 0; i < ProductImageService.MAX_IMAGES; i++) {
            images.add(product.getId(), png(300 + i, 200), null);
        }
        assertThatThrownBy(() -> images.add(product.getId(), png(300, 200), null))
                .isInstanceOf(ConflictException.class).hasMessageContaining("at most");

        ProductImage first = reloaded().getImages().get(0);
        images.delete(product.getId(), first.getId());
        assertThat(storage.find(first.getFileKey())).isEmpty();
        assertThat(storage.find(first.getThumbKey())).isEmpty();
        assertThat(reloaded().getImages()).hasSize(7).extracting(ProductImage::getPosition)
                .containsExactly(0, 1, 2, 3, 4, 5, 6);
    }

    @Test
    void deletingAProductRemovesItsImageFiles() throws Exception {
        ProductImage image = images.add(product.getId(), png(300, 200), null);
        productService.delete(product.getId());
        assertThat(storage.find(image.getFileKey())).isEmpty();
        assertThat(storage.find(image.getThumbKey())).isEmpty();
    }

    @Test
    void badUploadsAreRejectedWithAMessageAndStoreNothing() throws Exception {
        var asManager = user(new AppUserDetails(manager));
        mvc.perform(multipart("/manage/products/" + product.getId() + "/images")
                        .file(new MockMultipartFile("files", "evil.png", "image/png", "<script>alert(1)</script>".getBytes()))
                        .with(asManager).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(reloaded().getImages()).isEmpty();
    }

    @Test
    void onlyManagersMayChangeImagesAndTheApiListsThem() throws Exception {
        var asCustomer = user(new AppUserDetails(customer));
        mvc.perform(multipart("/manage/products/" + product.getId() + "/images")
                        .file(new MockMultipartFile("files", "a.png", "image/png", png(100, 100)))
                        .with(asCustomer).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/manage/products/" + product.getId() + "/images/1/delete").with(asCustomer).with(csrf()))
                .andExpect(status().isForbidden());

        ProductImage image = images.add(product.getId(), png(500, 400), "A lamp");
        mvc.perform(get("/api/products/" + product.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.images[0].url").value(image.getUrl()))
                .andExpect(jsonPath("$.images[0].alt").value("A lamp"));
    }
}