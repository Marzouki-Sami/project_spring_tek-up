package com.marzouki.shop.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductTypeRepository extends JpaRepository<ProductType, Long> {

    @EntityGraph(attributePaths = {"optionTypes"})
    List<ProductType> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"optionTypes", "optionTypes.values"})
    Optional<ProductType> findWithOptionsById(Long id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("select count(t) > 0 from ProductType t join t.optionTypes o where o.id = :optionTypeId")
    boolean usesOptionType(@Param("optionTypeId") Long optionTypeId);
}