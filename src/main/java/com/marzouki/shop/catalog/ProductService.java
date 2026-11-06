package com.marzouki.shop.catalog;

import com.marzouki.shop.catalog.dto.ProductForm;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository products;
    private final SubcategoryRepository subcategories;
    private final SupplierRepository suppliers;
    private final CatalogMapper mapper;

    public List<Product> findAll() {
        return products.findAllByOrderByNameAsc();
    }

    public Product get(Long id) {
        return products.findWithDetailsById(id).orElseThrow(() -> new NotFoundException("Product", id));
    }

    public ProductForm formFor(Long id) {
        return mapper.toForm(get(id));
    }

    @Transactional
    public Product create(ProductForm form) {
        if (products.existsBySkuIgnoreCase(form.getSku())) {
            throw new ConflictException("SKU '" + form.getSku() + "' is already used");
        }
        Product product = new Product();
        mapper.apply(form, product);
        attachRelations(product, form);
        return products.save(product);
    }

    @Transactional
    public Product update(Long id, ProductForm form) {
        Product product = get(id);
        if (products.existsBySkuIgnoreCaseAndIdNot(form.getSku(), id)) {
            throw new ConflictException("SKU '" + form.getSku() + "' is already used");
        }
        mapper.apply(form, product);
        attachRelations(product, form);
        return product;
    }

    @Transactional
    public void delete(Long id) {
        products.delete(get(id));
    }

    private void attachRelations(Product product, ProductForm form) {
        product.setSubcategory(subcategories.findById(form.getSubcategoryId())
                .orElseThrow(() -> new NotFoundException("Subcategory", form.getSubcategoryId())));
        product.setSupplier(form.getSupplierId() == null ? null
                : suppliers.findById(form.getSupplierId())
                        .orElseThrow(() -> new NotFoundException("Supplier", form.getSupplierId())));
    }
}