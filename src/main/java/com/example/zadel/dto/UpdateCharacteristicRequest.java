// ==================== НОВЫЙ ФАЙЛ: UpdateCharacteristicRequest.java ====================
package com.example.zadel.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class UpdateCharacteristicRequest {
    private String value;
    private UUID measureUid;
}