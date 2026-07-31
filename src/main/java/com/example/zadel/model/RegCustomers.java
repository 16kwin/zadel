package com.example.zadel.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reg_customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegCustomers {

    @Id
    private UUID uid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_uid")
    private SprMaterial material;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_uid")
    private SprCustomer customer;

    @Column(name = "supply_date")
    private LocalDateTime supplyDate;

    @Column(name = "document_name")
    private String documentName;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "original_name")
    private String originalName;
}