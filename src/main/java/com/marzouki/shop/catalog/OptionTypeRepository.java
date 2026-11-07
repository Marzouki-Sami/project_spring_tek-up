package com.marzouki.shop.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OptionTypeRepository extends JpaRepository<OptionType, Long> {

    @EntityGraph(attributePaths = "values")
    List<OptionType> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}