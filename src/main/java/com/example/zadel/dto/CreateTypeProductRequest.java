package com.example.zadel.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class CreateTypeProductRequest {
    private String name;
    private UUID typePurposeUid;
}