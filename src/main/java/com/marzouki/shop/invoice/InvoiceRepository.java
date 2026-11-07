package com.marzouki.shop.invoice;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByOrderId(Long orderId);

    @Query("select coalesce(max(i.sequence), 0) from Invoice i where i.year = :year")
    int highestSequence(@Param("year") int year);
}