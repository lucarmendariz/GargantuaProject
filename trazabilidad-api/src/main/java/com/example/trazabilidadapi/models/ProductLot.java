package com.example.trazabilidadapi.models;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class ProductLot {
    private int lotId;
    private int productId;
    private String lotNumber;
    private BigDecimal quantity;
    private BigDecimal reservedQuantity;
    private BigDecimal consumedQuantity;
    private BigDecimal currentStock;
    private BigDecimal availableQuantity;
    private BigDecimal inUseQuantity;
    private String measureUnit;
    private Long expirationDate;
    private String supplierReference;
    private Long createdAt;
    private Long updatedAt;
}
