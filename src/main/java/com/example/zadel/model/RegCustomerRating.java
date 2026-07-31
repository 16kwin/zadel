package com.example.zadel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reg_customer_ratings")
@Getter
@Setter
@NoArgsConstructor
public class RegCustomerRating {

    @Id
    @Column(name = "uid", nullable = false)
    private UUID uid;

    @ManyToOne
    @JoinColumn(name = "customer_uid", nullable = false)
    private SprCustomer customer;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;

    @Column(name = "author")
    private String author;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}