package com.msa.license.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FirmDto {
    private Long firmId;
    private String firmName;
    private LocalDate foundedDate;
}