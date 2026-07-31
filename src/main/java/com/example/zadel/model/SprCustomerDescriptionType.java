package com.example.zadel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "spr_customer_description_types")
@Getter
@Setter
@NoArgsConstructor
public class SprCustomerDescriptionType {

    @Id
    @Column(name = "uid", nullable = false)
    private UUID uid;

    @Column(name = "name", nullable = false)
    private String name;
}