// SupplierDescriptionTypeDTO.java
package com.example.zadel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDescriptionTypeDTO {
    private UUID uid;
    private String name;
}