package com.example.trazabilidadapi.models;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class ProductLot {
    private int lotId;
    private int productId;
    private String lotNumber;
    private Integer quantity;
    private String measureUnit;
    private Long expirationDate;
    private String supplierReference;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
