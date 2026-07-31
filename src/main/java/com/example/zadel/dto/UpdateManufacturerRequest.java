package com.example.zadel.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateManufacturerRequest {
    private String name;
    private String description;
}