package com.example.trazabilidadapi.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.example.trazabilidadapi.services.AccessService;
import com.example.trazabilidadapi.services.AccessService.UserAccess;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Predicate;

import static com.example.trazabilidadapi.services.AccessService.GESTIONAR;
import static com.example.trazabilidadapi.services.AccessService.VER;

@Configuration
public class SecurityConfig {

    private static final int MIN_SECRET_LENGTH = 32;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AccessService access) throws Exception {
        // Cada regla consulta en la BD si el usuario sigue activo, su rol y sus permisos (ver AccessService)
        Rule rule = check -> (auth, ctx) -> {
            // Solo un token JWT valido identifica al usuario (el anonimo de Spring tambien "esta autenticado")
            UserAccess user = auth.get() instanceof JwtAuthenticationToken token ? access.forUser(token.getName()) : null;
            return new AuthorizationDecision(user != null && check.test(user));
        };
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        // Paneles: el del proveedor solo para SUPERADMIN; el del cliente, para su ADMIN (y el SUPERADMIN, para dar soporte)
                        .requestMatchers("/api/plataforma/**").access(rule.of(u -> u.hasRole("SUPERADMIN")))
                        .requestMatchers("/api/admin/**").access(rule.of(u -> u.hasRole("ADMIN", "SUPERADMIN")))
                        // Pedidos. VER incluye prepararlos y entregarlos (trabajo de planta); GESTIONAR, crearlos, asignar lotes y cancelarlos
                        .requestMatchers(HttpMethod.GET, "/api/pedidos/**").access(rule.of(u -> u.can(VER, "PEDIDOS")))
                        .requestMatchers(HttpMethod.POST, "/api/pedidos/*/preparar", "/api/pedidos/*/entregar").access(rule.of(u -> u.can(VER, "PEDIDOS")))
                        .requestMatchers("/api/pedidos/**").access(rule.of(u -> u.can(GESTIONAR, "PEDIDOS")))
                        // Clientes: se consultan con los pedidos; darlos de alta es de quien crea pedidos
                        .requestMatchers(HttpMethod.GET, "/api/clientes").access(rule.of(u -> u.can(VER, "PEDIDOS")))
                        .requestMatchers("/api/clientes/**").access(rule.of(u -> u.can(GESTIONAR, "PEDIDOS")))
                        // Produccion registra movimientos; el historial solo se consulta
                        .requestMatchers(HttpMethod.POST, "/api/historial/escanear").access(rule.of(u -> u.can(VER, "PRODUCCION")))
                        .requestMatchers("/api/historial/**").access(rule.of(u -> u.can(VER, "HISTORIAL")))
                        // Trazabilidad de un lote (Panel Central, pedidos, lotes): cualquier usuario activo
                        .requestMatchers(HttpMethod.GET, "/api/trazabilidad/**", "/api/lotes/*/trazabilidad").access(rule.of(u -> true))
                        // Catalogo y lotes: se leen desde Inventario, Lotes, al asignar lotes a un pedido y al elegir el lote en Produccion
                        .requestMatchers(HttpMethod.GET, "/api/productos/**", "/api/lotes/**").access(rule.of(u -> u.can(VER, "INVENTARIO", "LOTES", "PRODUCCION") || u.can(GESTIONAR, "PEDIDOS")))
                        .requestMatchers(HttpMethod.POST, "/api/lotes").access(rule.of(u -> u.can(GESTIONAR, "INVENTARIO", "LOTES")))
                        .requestMatchers("/api/lotes/**").access(rule.of(u -> u.can(GESTIONAR, "LOTES")))
                        .requestMatchers("/api/productos/**").access(rule.of(u -> u.can(GESTIONAR, "INVENTARIO")))
                        .anyRequest().access(rule.of(u -> true)))
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        return http.build();
    }

    /** Regla de acceso a partir de los permisos del usuario. */
    private interface Rule {
        AuthorizationManager<RequestAuthorizationContext> of(Predicate<UserAccess> check);
    }

    @Bean
    public SecretKey jwtSecretKey(@Value("${app.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos " + MIN_SECRET_LENGTH + " caracteres.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
