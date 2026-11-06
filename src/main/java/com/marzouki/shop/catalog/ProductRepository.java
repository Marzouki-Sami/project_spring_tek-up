package com.marzouki.shop.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"subcategory", "subcategory.category", "supplier"})
    List<Product> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"subcategory", "subcategory.category", "supplier"})
    Optional<Product> findWithDetailsById(Long id);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    boolean existsBySubcategoryId(Long subcategoryId);

    boolean existsBySupplierId(Long supplierId);
}