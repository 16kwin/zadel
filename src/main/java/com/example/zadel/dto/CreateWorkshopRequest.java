// CreateWorkshopRequest.java
package com.example.zadel.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWorkshopRequest {
    private String name;
    private Long enterpriseId;
}