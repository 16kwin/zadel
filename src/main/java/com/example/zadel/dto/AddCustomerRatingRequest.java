// AddSupplierRatingRequest.java
package com.example.zadel.dto;

import lombok.Data;

@Data
public class AddCustomerRatingRequest {
    private Integer rating;
    private String comment;
    private String author;
}