package com.example.trazabilidadapi.models;

import lombok.*;

@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class CreateOrderRequest {
    private String orderNumber;
    private Integer customerId;
    private Long scheduledDate;
    private String notes;
}
