package com.example.zadel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerEventLogDTO {
    private UUID uid;
    private UUID customerUid;
    private String eventType;
    private String eventDescription;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private String author;
    private String source;
    private LocalDateTime createdAt;
}