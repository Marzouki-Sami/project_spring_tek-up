package com.marzouki.shop.catalog;

import com.marzouki.shop.catalog.dto.SubcategoryForm;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubcategoryService {

    private final SubcategoryRepository subcategories;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final CatalogMapper mapper;

    public List<Subcategory> findAll() {
        return subcategories.findAllByOrderByCategoryTitleAscTitleAsc();
    }

    public Subcategory get(Long id) {
        return subcategories.findById(id).orElseThrow(() -> new NotFoundException("Subcategory", id));
    }

    public SubcategoryForm formFor(Long id) {
        Subcategory subcategory = subcategories.findById(id)
                .orElseThrow(() -> new NotFoundException("Subcategory", id));
        return mapper.toForm(subcategory);
    }

    @Transactional
    public Subcategory create(SubcategoryForm form) {
        Category category = categories.findById(form.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category", form.getCategoryId()));
        if (subcategories.existsByCategoryIdAndTitleIgnoreCase(category.getId(), form.getTitle())) {
            throw new ConflictException("'" + category.getTitle() + "' already has a subcategory named '"
                    + form.getTitle() + "'");
        }
        Subcategory subcategory = new Subcategory();
        mapper.apply(form, subcategory);
        subcategory.setCategory(category);
        return subcategories.save(subcategory);
    }

    @Transactional
    public Subcategory update(Long id, SubcategoryForm form) {
        Subcategory subcategory = get(id);
        Category category = categories.findById(form.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category", form.getCategoryId()));
        boolean renamedOrMoved = !subcategory.getTitle().equalsIgnoreCase(form.getTitle())
                || !subcategory.getCategory().getId().equals(category.getId());
        if (renamedOrMoved
                && subcategories.existsByCategoryIdAndTitleIgnoreCase(category.getId(), form.getTitle())) {
            throw new ConflictException("'" + category.getTitle() + "' already has a subcategory named '"
                    + form.getTitle() + "'");
        }
        mapper.apply(form, subcategory);
        subcategory.setCategory(category);
        return subcategory;
    }

    @Transactional
    public void delete(Long id) {
        Subcategory subcategory = get(id);
        if (products.existsBySubcategoryId(id)) {
            throw new ConflictException("Move or remove the products in '" + subcategory.getTitle() + "' first");
        }
        subcategories.delete(subcategory);
    }
}