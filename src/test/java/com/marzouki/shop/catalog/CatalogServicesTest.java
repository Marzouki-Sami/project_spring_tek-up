package com.marzouki.shop.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marzouki.shop.catalog.dto.CategoryForm;
import com.marzouki.shop.catalog.dto.ProductForm;
import com.marzouki.shop.catalog.dto.SubcategoryForm;
import com.marzouki.shop.catalog.dto.SupplierForm;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.NotFoundException;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CatalogServicesTest {

    @Autowired CategoryService categories;
    @Autowired SubcategoryService subcategories;
    @Autowired SupplierService suppliers;
    @Autowired ProductService products;

    private Subcategory subcategory;

    @BeforeEach
    void setUp() {
        CategoryForm category = new CategoryForm();
        category.setTitle("Electronics");
        Category saved = categories.create(category);

        SubcategoryForm sub = new SubcategoryForm();
        sub.setTitle("Phones");
        sub.setCategoryId(saved.getId());
        subcategory = subcategories.create(sub);
    }

    private ProductForm productForm(String sku) {
        ProductForm form = new ProductForm();
        form.setSku(sku);
        form.setName("Phone " + sku);
        form.setPrice(new BigDecimal("199.90"));
        form.setStock(5);
        form.setSubcategoryId(subcategory.getId());
        return form;
    }

    @Test
    void createsAProductWithItsRelations() {
        SupplierForm supplier = new SupplierForm();
        supplier.setCompany("Acme Ltd");
        Supplier savedSupplier = suppliers.create(supplier);
        ProductForm form = productForm("PH-1");
        form.setSupplierId(savedSupplier.getId());

        Product product = products.create(form);

        Product loaded = products.get(product.getId());
        assertThat(loaded.getSku()).isEqualTo("PH-1");
        assertThat(loaded.getSupplier().getCompany()).isEqualTo("Acme Ltd");
        assertThat(loaded.getSubcategory().getCategory().getTitle()).isEqualTo("Electronics");
    }

    @Test
    void rejectsADuplicateSkuIgnoringCase() {
        products.create(productForm("PH-1"));

        assertThatThrownBy(() -> products.create(productForm("ph-1")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updatingAProductKeepsItsOwnSkuAndItsStock() {
        Product product = products.create(productForm("PH-1"));
        ProductForm form = products.formFor(product.getId());
        form.setName("Renamed phone");
        form.setStock(99);

        Product updated = products.update(product.getId(), form);

        assertThat(updated.getName()).isEqualTo("Renamed phone");
        assertThat(updated.getStock()).isEqualTo(5);
        assertThat(updated.getSku()).isEqualTo("PH-1");
    }

    @Test
    void updatingToAnotherProductsSkuIsRejected() {
        products.create(productForm("PH-1"));
        Product other = products.create(productForm("PH-2"));
        ProductForm form = products.formFor(other.getId());
        form.setSku("PH-1");

        assertThatThrownBy(() -> products.update(other.getId(), form))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void anUnknownProductIsNotFound() {
        assertThatThrownBy(() -> products.get(999_999L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void aCategoryWithSubcategoriesCannotBeDeleted() {
        Long categoryId = subcategory.getCategory().getId();

        assertThatThrownBy(() -> categories.delete(categoryId)).isInstanceOf(ConflictException.class);
    }

    @Test
    void aSubcategoryWithProductsCannotBeDeleted() {
        products.create(productForm("PH-1"));

        assertThatThrownBy(() -> subcategories.delete(subcategory.getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void aSupplierWithProductsCannotBeDeleted() {
        SupplierForm supplier = new SupplierForm();
        supplier.setCompany("Acme Ltd");
        Supplier savedSupplier = suppliers.create(supplier);
        ProductForm form = productForm("PH-1");
        form.setSupplierId(savedSupplier.getId());
        products.create(form);

        assertThatThrownBy(() -> suppliers.delete(savedSupplier.getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void aDuplicateCategoryTitleIsRejectedIgnoringCase() {
        CategoryForm form = new CategoryForm();
        form.setTitle("ELECTRONICS");

        assertThatThrownBy(() -> categories.create(form)).isInstanceOf(ConflictException.class);
    }
}