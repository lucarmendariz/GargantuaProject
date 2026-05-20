package com.example.trazabilidadapi.models;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class CurrentLotStatus {
    private int productId;
    private String productSku;
    private String productName;
    private int lotId;
    private String lotNumber;
    private BigDecimal initialQuantity;
    private String measureUnit;
    private LocalDate expirationDate;
    private String currentState;
    private LocalDateTime lastUpdate;
    private String lastOperator;
    private String ocrRawText;
    private String lastObservations;

}