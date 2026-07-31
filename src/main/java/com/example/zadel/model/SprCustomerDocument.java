package com.example.zadel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "spr_customer_documents")
@Getter
@Setter
@NoArgsConstructor
public class SprCustomerDocument {

    @Id
    @Column(name = "uid", nullable = false)
    private UUID uid;

    @ManyToOne
    @JoinColumn(name = "customer_uid", nullable = false)
    private SprCustomer customer;

    @Column(name = "document_name", nullable = false)
    private String documentName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}