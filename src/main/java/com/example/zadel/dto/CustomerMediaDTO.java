// SupplierMediaDTO.java
package com.example.zadel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerMediaDTO {
    private UUID uid;
    private UUID supplierUid;
    private String filePath;
    private String originalName;
    private String fileUrl;
    private Integer sortOrder;
}