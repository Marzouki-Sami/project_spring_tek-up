package com.marzouki.shop.mail;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** An edited email. Emails nobody has edited have no row and use their built-in wording. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "email_templates")
public class EmailTemplate {

    @Id
    @Column(name = "template_key", length = 40)
    private String key;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 120)
    private String heading;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(name = "button_label", length = 40)
    private String buttonLabel;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "updated_by")
    private Long updatedBy;
}