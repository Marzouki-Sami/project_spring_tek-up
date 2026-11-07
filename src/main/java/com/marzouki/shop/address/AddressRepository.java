package com.marzouki.shop.address;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserIdOrderByDefaultAddressDescCreatedAtDescIdDesc(Long userId);

    Optional<Address> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Address a set a.defaultAddress = false where a.user.id = :userId and a.defaultAddress = true")
    int clearDefault(@Param("userId") Long userId);
}