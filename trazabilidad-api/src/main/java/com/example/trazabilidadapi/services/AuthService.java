package com.example.trazabilidadapi.services;

import com.example.trazabilidadapi.models.AppUser;
import com.example.trazabilidadapi.models.LoginResponse;
import com.example.trazabilidadapi.repositories.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LoginResponse authenticate(String username, String password) {
        // 1. Buscar si el usuario existe en la BD
        AppUser user = userRepository.findByUsername(username);

        if (user == null) {
            throw new IllegalArgumentException("Usuario o contraseña incorrectos.");
        }

        // 2. Validar la contraseña (Comparación directa por ahora)
        // NOTA: Si en el futuro usas BCrypt, aquí usarías encoder.matches(password, user.getPasswordHash())
        if (!user.getPasswordHash().equals(password)) {
            throw new IllegalArgumentException("Usuario o contraseña incorrectos.");
        }

        // 3. Si todo es correcto, devolvemos la respuesta de éxito sin la contraseña por seguridad
        return new LoginResponse(
                user.getUsername(),
                user.getName(),
                user.getRole(),
                "¡Autenticación en MonCorp exitosa!"
        );
    }
}