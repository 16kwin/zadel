package com.example.zadel.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class CreateTypePurposeRequest {
    private String name;
    private UUID typeMaterialUid;
}