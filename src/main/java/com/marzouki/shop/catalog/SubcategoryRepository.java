package com.marzouki.shop.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {

    @EntityGraph(attributePaths = "category")
    List<Subcategory> findAllByOrderByCategoryTitleAscTitleAsc();

    boolean existsByCategoryIdAndTitleIgnoreCase(Long categoryId, String title);

    boolean existsByCategoryId(Long categoryId);
}