package com.marzouki.shop.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByTitleIgnoreCase(String title);

    @EntityGraph(attributePaths = "subcategories")
    List<Category> findAllByOrderByTitleAsc();
}