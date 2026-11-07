package com.marzouki.shop.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "option_values")
public class OptionValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_type_id")
    private OptionType optionType;

    @Column(nullable = false, length = 60)
    private String label;

    /** Only for COLOR options: the swatch colour as #rrggbb. */
    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(nullable = false)
    private int position;
}