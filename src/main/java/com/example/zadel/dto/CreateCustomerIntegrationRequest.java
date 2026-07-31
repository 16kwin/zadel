// CreateSupplierIntegrationRequest.java
package com.example.zadel.dto;

import lombok.Data;

@Data
public class CreateCustomerIntegrationRequest {
    private String exchangeType;
    private String direction;
    private String protocol;
    private String targetSystem;
}