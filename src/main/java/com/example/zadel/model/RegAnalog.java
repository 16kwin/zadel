// RegAnalog.java
package com.example.zadel.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reg_analog")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegAnalog {

    @Id
    private UUID uid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_uid", nullable = false)
    private SprMaterial material;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analog_material_uid", nullable = false)
    private SprMaterial analogMaterial;

    @Column(name = "compatibility_percent", nullable = false)
    private Integer compatibilityPercent;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}