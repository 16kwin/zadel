// SectionFlatDto.java
package com.example.zadel.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SectionFlatDto {
    private Long id;
    private String name;
    private Long holdingId;
    private String holdingName;
    private Long enterpriseId;
    private String enterpriseName;
    private Long workshopId;
    private String workshopName;
}