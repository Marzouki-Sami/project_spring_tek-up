package com.marzouki.shop.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A company that supplies products to the store. Not a login account. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String company;

    @Column(name = "contact_name", length = 120)
    private String contactName;

    @Column(length = 254)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(name = "registration_number", length = 60)
    private String registrationNumber;

    @Column(nullable = false)
    private boolean active = true;
}