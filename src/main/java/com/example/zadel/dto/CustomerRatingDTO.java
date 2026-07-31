// SupplierRatingDTO.java
package com.example.zadel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRatingDTO {
    private UUID uid;
    private UUID supplierUid;
    private Integer rating;
    private String comment;
    private String author;
    private LocalDateTime createdAt;
}