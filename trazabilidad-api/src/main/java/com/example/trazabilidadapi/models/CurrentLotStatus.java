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
    private Integer initialQuantity;
    private String measureUnit;
    private Integer expirationDate;
    private String currentState;
    private Integer lastUpdate;
    private String lastOperator;
    private String ocrRawText;
    private String lastObservations;

}