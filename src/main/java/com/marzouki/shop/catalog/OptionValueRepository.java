package com.marzouki.shop.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OptionValueRepository extends JpaRepository<OptionValue, Long> {

    @Query("select count(v) > 0 from ProductVariant v join v.optionValues o where o.id = :valueId")
    boolean isUsed(@Param("valueId") Long valueId);

    @Query("select count(v) > 0 from ProductVariant v join v.optionValues o where o.optionType.id = :typeId")
    boolean isTypeUsed(@Param("typeId") Long typeId);
}