// dto/NomenclatureCreateResponse.java
package com.example.zadel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class NomenclatureCreateResponse {
    private UUID uid;
    private Integer code;
}