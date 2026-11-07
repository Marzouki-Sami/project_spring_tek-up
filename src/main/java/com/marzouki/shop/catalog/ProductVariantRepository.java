package com.marzouki.shop.catalog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    /** Variants a customer may buy right now, with what the cart and checkout pages show about them. */
    @Query("select distinct v from ProductVariant v join fetch v.product p join fetch p.subcategory s join fetch s.category "
            + "left join fetch v.optionValues ov left join fetch ov.optionType "
            + "where v.id in :ids and v.active = true and p.active = true")
    List<ProductVariant> findPurchasable(@Param("ids") Collection<Long> ids);

    @Query("select distinct v from ProductVariant v join fetch v.product p join fetch p.subcategory s join fetch s.category "
            + "left join fetch v.optionValues ov left join fetch ov.optionType "
            + "where v.id = :id and v.active = true and p.active = true")
    Optional<ProductVariant> findPurchasableById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"product", "optionValues", "optionValues.optionType"})
    Optional<ProductVariant> findWithDetailsById(Long id);

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    @Query("select distinct v from ProductVariant v join fetch v.product p left join fetch v.optionValues ov "
            + "left join fetch ov.optionType where v.active = true and p.active = true "
            + "and v.stock <= :level order by v.stock asc, p.name asc")
    List<ProductVariant> lowStock(@Param("level") int level, Pageable limit);

    /** Takes stock only if enough is left, in one atomic statement so two buyers can never oversell. */
    @Modifying(flushAutomatically = true)
    @Query("update ProductVariant v set v.stock = v.stock - :quantity where v.id = :id and v.active = true "
            + "and v.stock >= :quantity and exists (select 1 from Product p where p.id = v.product.id and p.active = true)")
    int takeStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying(flushAutomatically = true)
    @Query("update ProductVariant v set v.stock = v.stock + :quantity where v.id = :id")
    int returnStock(@Param("id") Long id, @Param("quantity") int quantity);

    /** Changes stock by a signed amount, refusing to go below zero. */
    @Modifying(flushAutomatically = true)
    @Query("update ProductVariant v set v.stock = v.stock + :delta where v.id = :id and v.stock + :delta >= 0")
    int adjust(@Param("id") Long id, @Param("delta") int delta);
}