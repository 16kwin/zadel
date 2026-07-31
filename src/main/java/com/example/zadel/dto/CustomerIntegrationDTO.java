// SupplierIntegrationDTO.java
package com.example.zadel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerIntegrationDTO {
    private UUID uid;
    private UUID supplierUid;
    private String event;
    private String exchangeType;
    private String direction;
    private String protocol;
    private String targetSystem;
    private LocalDateTime createdAt;
}