package com.marzouki.shop.catalog;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String title;

    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "category")
    @OrderBy("title")
    private List<Subcategory> subcategories = new ArrayList<>();
}