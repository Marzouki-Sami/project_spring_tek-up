package com.marzouki.shop.catalog;

import com.marzouki.shop.catalog.dto.ProductTypeForm;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductTypeService {

    private final ProductTypeRepository types;
    private final OptionTypeRepository optionTypes;
    private final ProductRepository products;

    public List<ProductType> findAll() {
        return types.findAllByOrderByNameAsc();
    }

    public ProductTypeForm formFor(Long id) {
        ProductType type = types.findWithOptionsById(id).orElseThrow(() -> new NotFoundException("Product type", id));
        ProductTypeForm form = new ProductTypeForm();
        form.setName(type.getName());
        form.setOptionTypeIds(type.getOptionTypes().stream().map(OptionType::getId).toList());
        return form;
    }

    @Transactional
    public ProductType create(ProductTypeForm form) {
        String name = form.getName().trim();
        if (types.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A product type called '" + name + "' already exists");
        }
        ProductType type = new ProductType();
        type.setName(name);
        type.setOptionTypes(new java.util.LinkedHashSet<>(optionTypes.findAllById(form.getOptionTypeIds())));
        return types.save(type);
    }

    @Transactional
    public ProductType update(Long id, ProductTypeForm form) {
        ProductType type = types.findWithOptionsById(id).orElseThrow(() -> new NotFoundException("Product type", id));
        String name = form.getName().trim();
        if (types.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("A product type called '" + name + "' already exists");
        }
        List<OptionType> wanted = optionTypes.findAllById(form.getOptionTypeIds());
        boolean removesOption = type.getOptionTypes().stream().anyMatch(o -> !wanted.contains(o));
        if (removesOption && products.existsByProductTypeId(id)) {
            throw new ConflictException("Products already use this type, so its options can only be added to");
        }
        type.setName(name);
        type.setOptionTypes(new java.util.LinkedHashSet<>(wanted));
        return type;
    }

    @Transactional
    public void delete(Long id) {
        ProductType type = types.findById(id).orElseThrow(() -> new NotFoundException("Product type", id));
        if (products.existsByProductTypeId(id)) {
            throw new ConflictException("Products use '" + type.getName() + "', so it cannot be deleted");
        }
        types.delete(type);
    }
}