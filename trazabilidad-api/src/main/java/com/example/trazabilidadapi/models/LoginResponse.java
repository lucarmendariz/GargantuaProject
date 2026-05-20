package com.example.trazabilidadapi.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String username;
    private String name;
    private String role;
    private String message;
}