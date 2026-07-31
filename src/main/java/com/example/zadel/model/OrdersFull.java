package com.example.zadel.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "zadel_orders_full")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdersFull {
    @Id
    @Column(name = "order_uid")
    private String orderUid;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "order_json", columnDefinition = "jsonb", nullable = false)
    private String orderJson;
}