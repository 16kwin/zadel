// CalculateCompatibilityRequest.java
package com.example.zadel.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class CalculateCompatibilityRequest {
    private UUID materialUid1;
    private UUID materialUid2;
}