package com.marzouki.shop.catalog;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A kind of choice a product can come in, such as Size or Color. Shared by many product types. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "option_types")
public class OptionType {

    public enum Display { TEXT, COLOR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    /** TEXT shows buttons with the label; COLOR shows round swatches. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Display display = Display.TEXT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "optionType", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position, id")
    private List<OptionValue> values = new ArrayList<>();

    public OptionValue addValue(String label, String colorHex) {
        OptionValue value = new OptionValue();
        value.setOptionType(this);
        value.setLabel(label);
        value.setColorHex(colorHex);
        value.setPosition(values.size());
        values.add(value);
        return value;
    }
}