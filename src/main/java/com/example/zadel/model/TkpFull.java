package com.example.zadel.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "zadel_tkp_full")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TkpFull {
    @Id
    @Column(name = "tkp_uid")
    private String tkpUid;

    @Column(name = "order_uid", nullable = false)
    private String orderUid;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tkp_json", columnDefinition = "jsonb", nullable = false)
    private String tkpJson;
}