package com.example.zadel.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMeasureRequest {
    private String name;
    private String description;
}