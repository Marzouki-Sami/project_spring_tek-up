package com.example.demo.service;

import com.example.demo.model.SubCategory;
import com.example.demo.repository.SubCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SubCategoryService {

    @Autowired
    private SubCategoryRepository subCategoryRepository;

    public SubCategory getSubCategoryById(Long id) {
        if (subCategoryRepository.findById(id).isPresent())
            return subCategoryRepository.findById(id).get();
        else
            throw new RuntimeException("SubCategory not found");
    }

    public void deleteSubCategoryById(Long id) {
        subCategoryRepository.deleteById(id);
    }

    public void updateSubCategory(SubCategory subCategory) {
        SubCategory existingSubCategory = getSubCategoryById(subCategory.getId_subcategory());

        if (subCategory.getTitle() != null) {
            existingSubCategory.setTitle(subCategory.getTitle());
        }
        if (subCategory.getDescription() != null) {
            existingSubCategory.setDescription(subCategory.getDescription());
        }
        if (subCategory.getCategory() != null) {
            existingSubCategory.setCategory(subCategory.getCategory());
        }

        subCategoryRepository.saveAndFlush(existingSubCategory);
    }

    public boolean subCategoryExist(Long id) {
        return subCategoryRepository.existsById(id);
    }

    public void addSubCategory(SubCategory subCategory) {
        subCategoryRepository.save(subCategory);
    }
    
}
