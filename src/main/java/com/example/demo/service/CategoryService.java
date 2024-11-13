package com.example.demo.service;

import com.example.demo.model.Category;
import com.example.demo.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public Category getCategoryById(Long id) {
        if (categoryRepository.findById(id).isPresent())
            return categoryRepository.findById(id).get();
        else
            throw new RuntimeException("Category not found");
    }

    public void deleteCategoryById(Long id) {
        categoryRepository.deleteById(id);
    }

    public void updateCategory(Category category) {
        Category existingCategory = getCategoryById(category.getId_category());

        if (category.getTitle() != null) {
            existingCategory.setTitle(category.getTitle());
        }
        if (category.getDescription() != null) {
            existingCategory.setDescription(category.getDescription());
        }

        categoryRepository.saveAndFlush(existingCategory);
    }

    public boolean categoryExist(Long id) {
        return categoryRepository.existsById(id);
    }

    public void addCategory(Category category) {
        categoryRepository.save(category);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
}
