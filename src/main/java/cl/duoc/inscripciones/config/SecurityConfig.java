package cl.duoc.inscripciones.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilita CSRF ya que es una API REST y se maneja con tokens, no con cookies de sesión
            .csrf(csrf -> csrf.disable())
            
            // Fuerza a Spring Security a no guardar estados de sesión en memoria (Evita el comportamiento fantasma)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Reglas de autorización de rutas
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/publico").permitAll() // Ruta libre de pruebas
                .anyRequest().authenticated() // Todo lo demás (incluyendo tu controlador) exige token válido
            )
            
            // Habilita el servidor de recursos OAuth2 usando tokens JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {}) // Validación automática contra el JWK Set configurado abajo
            );
        
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Endpoint público de Azure para verificar las firmas criptográficas de los tokens emitidos
        String jwkSetUri = "https://login.microsoftonline.com/ee32ed64-c270-4007-8d64-8598998d3959/discovery/v2.0/keys";
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}