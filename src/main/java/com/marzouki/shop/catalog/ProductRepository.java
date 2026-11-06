package com.marzouki.shop.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"subcategory", "subcategory.category", "supplier"})
    List<Product> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"subcategory", "subcategory.category", "supplier"})
    Optional<Product> findWithDetailsById(Long id);

    /** Storefront search: active products only, optionally narrowed by text and category. */
    @EntityGraph(attributePaths = {"subcategory", "subcategory.category"})
    @Query("""
            select p from Product p
            where p.active = true
              and (lower(p.name) like lower(concat('%', :text, '%')) or lower(p.description) like lower(concat('%', :text, '%')))
              and (:categoryId is null or p.subcategory.category.id = :categoryId)
            """)
    Page<Product> search(@Param("text") String text, @Param("categoryId") Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"subcategory", "subcategory.category"})
    List<Product> findTop8ByActiveTrueOrderByCreatedAtDescIdDesc();

    @EntityGraph(attributePaths = {"subcategory", "subcategory.category"})
    Optional<Product> findByIdAndActiveTrue(Long id);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    boolean existsBySubcategoryId(Long subcategoryId);

    boolean existsBySupplierId(Long supplierId);
}