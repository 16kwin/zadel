package com.example.zadel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reg_price")
@Getter
@Setter
@NoArgsConstructor
public class RegPrice {

    @Id
    @Column(name = "uid", nullable = false)
    private UUID uid;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "price_date")
    private LocalDateTime priceDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link")
    private SprMaterial material;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_entrance_uid")
    private DocEntrance docEntrance;
}