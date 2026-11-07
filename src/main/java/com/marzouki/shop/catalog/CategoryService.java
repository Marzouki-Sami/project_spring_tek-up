package com.marzouki.shop.catalog;

import com.marzouki.shop.catalog.dto.CategoryForm;
import com.marzouki.shop.common.ConflictException;
import com.marzouki.shop.common.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categories;
    private final SubcategoryRepository subcategories;
    private final CatalogMapper mapper;

    public org.springframework.data.domain.Page<Category> search(com.marzouki.shop.common.ListQuery query) {
        var page = categories.findAll(com.marzouki.shop.common.Specs.<Category>text(query.like(), "title", "description"),
                query.pageRequest(org.springframework.data.domain.Sort.by("title").ascending().and(org.springframework.data.domain.Sort.by("id"))));
        page.forEach(c -> c.getSubcategories().size());
        return page;
    }

    public List<Category> findAll() {
        return categories.findAllByOrderByTitleAsc();
    }

    public Category get(Long id) {
        return categories.findById(id).orElseThrow(() -> new NotFoundException("Category", id));
    }

    public CategoryForm formFor(Long id) {
        return mapper.toForm(get(id));
    }

    @Transactional
    public Category create(CategoryForm form) {
        ensureTitleFree(form.getTitle());
        Category category = new Category();
        mapper.apply(form, category);
        return categories.save(category);
    }

    @Transactional
    public Category update(Long id, CategoryForm form) {
        Category category = get(id);
        if (!category.getTitle().equalsIgnoreCase(form.getTitle())) {
            ensureTitleFree(form.getTitle());
        }
        mapper.apply(form, category);
        return category;
    }

    @Transactional
    public void delete(Long id) {
        Category category = get(id);
        if (subcategories.existsByCategoryId(id)) {
            throw new ConflictException("Remove the subcategories of '" + category.getTitle() + "' first");
        }
        categories.delete(category);
    }

    private void ensureTitleFree(String title) {
        if (categories.existsByTitleIgnoreCase(title)) {
            throw new ConflictException("A category named '" + title + "' already exists");
        }
    }
}