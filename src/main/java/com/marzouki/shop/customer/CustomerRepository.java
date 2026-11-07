package com.marzouki.shop.customer;

import com.marzouki.shop.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Read-only queries over customers; one grouped query per page, never one per customer. */
public interface CustomerRepository extends JpaRepository<User, Long> {

    String SELECT = "select new com.marzouki.shop.customer.CustomerRow(u.id, u.firstName, u.lastName, u.email, u.enabled, "
            + "u.createdAt, count(o.id), coalesce(sum(o.total), 0), max(o.createdAt)) from User u "
            + "left join Order o on o.user = u and o.status <> com.marzouki.shop.order.OrderStatus.CANCELLED "
            + "where u.role = com.marzouki.shop.user.Role.CUSTOMER and (:q = '' or lower(u.email) like :q escape '\\' "
            + "or lower(u.firstName) like :q escape '\\' or lower(u.lastName) like :q escape '\\' "
            + "or lower(concat(u.firstName, ' ', u.lastName)) like :q escape '\\') "
            + "group by u.id, u.firstName, u.lastName, u.email, u.enabled, u.createdAt ";

    String COUNT = "select count(u) from User u where u.role = com.marzouki.shop.user.Role.CUSTOMER and (:q = '' "
            + "or lower(u.email) like :q escape '\\' or lower(u.firstName) like :q escape '\\' "
            + "or lower(u.lastName) like :q escape '\\' or lower(concat(u.firstName, ' ', u.lastName)) like :q escape '\\')";

    @Query(value = SELECT + "order by u.lastName, u.firstName, u.id", countQuery = COUNT)
    Page<CustomerRow> byName(@Param("q") String q, Pageable pageable);

    @Query(value = SELECT + "order by u.createdAt desc, u.id desc", countQuery = COUNT)
    Page<CustomerRow> byNewest(@Param("q") String q, Pageable pageable);

    @Query(value = SELECT + "order by coalesce(sum(o.total), 0) desc, u.id", countQuery = COUNT)
    Page<CustomerRow> bySpent(@Param("q") String q, Pageable pageable);

    @Query(value = SELECT + "order by count(o.id) desc, u.id", countQuery = COUNT)
    Page<CustomerRow> byOrders(@Param("q") String q, Pageable pageable);

    @Query("select new com.marzouki.shop.customer.CustomerRow(u.id, u.firstName, u.lastName, u.email, u.enabled, "
            + "u.createdAt, count(o.id), coalesce(sum(o.total), 0), max(o.createdAt)) from User u "
            + "left join Order o on o.user = u and o.status <> com.marzouki.shop.order.OrderStatus.CANCELLED "
            + "where u.id = :id and u.role = com.marzouki.shop.user.Role.CUSTOMER "
            + "group by u.id, u.firstName, u.lastName, u.email, u.enabled, u.createdAt")
    java.util.Optional<CustomerRow> summary(@Param("id") Long id);
}