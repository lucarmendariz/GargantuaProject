package com.example.trazabilidadapi.models;

import lombok.*;

@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class AppUser {
    private int userId;
    private String username;
    private String passwordHash;
    private String name;
    private String role;
    private boolean isActive;
}