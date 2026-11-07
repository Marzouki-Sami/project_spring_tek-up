package com.marzouki.shop.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubcategoryRepository extends JpaRepository<Subcategory, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Subcategory> {

    @EntityGraph(attributePaths = "category")
    @Override
    org.springframework.data.domain.Page<Subcategory> findAll(org.springframework.data.jpa.domain.Specification<Subcategory> spec, org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = "category")
    List<Subcategory> findAllByOrderByCategoryTitleAscTitleAsc();

    boolean existsByCategoryIdAndTitleIgnoreCase(Long categoryId, String title);

    boolean existsByCategoryId(Long categoryId);

    List<Subcategory> findByCategoryIdOrderByTitleAsc(Long categoryId);
}