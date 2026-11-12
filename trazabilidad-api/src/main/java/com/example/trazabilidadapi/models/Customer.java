package com.example.trazabilidadapi.models;

import lombok.*;

@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class Customer {
    private int customerId;
    private String name;
    private Long createdAt;
    private int orderCount;
}
