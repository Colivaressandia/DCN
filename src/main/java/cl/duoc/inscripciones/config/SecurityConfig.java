package cl.duoc.inscripciones.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilita CSRF ya que es una API REST y se maneja con tokens, no con cookies de sesión
            .csrf(csrf -> csrf.disable())
            
            // Fuerza a Spring Security a no guardar estados de sesión en memoria
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Reglas de autorización de rutas (Perfilamiento por roles exigido por la pauta)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/publico").permitAll() // Ruta libre de pruebas
                
                // Endpoint de descargar guías restringido solo al rol "consulta"
                .requestMatchers(HttpMethod.GET, "/api/guias/descargar").hasAuthority("ROLE_consulta")
                
                // El resto de los endpoints requieren cualquier otro rol de edición/administración o autenticación genérica
                .anyRequest().authenticated() 
            )
            
            // Habilita el servidor de recursos OAuth2 vinculando nuestro convertidor de roles
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );
        
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Dirección oficial de descubrimiento de llaves públicas de TU Azure AD B2C
        String jwkSetUri = "https://cristianolivaresapi.b2clogin.com/cristianolivaresapi.onmicrosoft.com/B2C_1_signup_signin/discovery/v2.0/keys";
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        // Configura la extracción del claim personalizado 'extension_consultaRole' de Azure B2C
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("extension_consultaRole");
        // Agrega el prefijo estándar 'ROLE_' para que funcione con la directiva .hasAuthority("ROLE_consulta")
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
}