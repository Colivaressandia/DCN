package cl.duoc.inscripciones.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.convert.converter.Converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/publico").permitAll()
                
                // REGLAS DE ROLES:
                // 1. Para crear, subir, editar y borrar guías (POST, PUT, DELETE) se exige ROLE_ADMIN
                .requestMatchers(HttpMethod.POST, "/api/guias/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/guias/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/guias/**").hasAuthority("ROLE_ADMIN")
                
                // 2. Para descargar o buscar guías (GET) basta con tener ROLE_DESCARGAR
                .requestMatchers(HttpMethod.GET, "/api/guias/*/download").hasAuthority("ROLE_DESCARGAR")
                .requestMatchers(HttpMethod.GET, "/api/guias").hasAuthority("ROLE_DESCARGAR")
                .requestMatchers(HttpMethod.GET, "/api/guias/buscar").hasAuthority("ROLE_DESCARGAR")
                
                .anyRequest().authenticated() 
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );
        
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String jwkSetUri = "https://cristianolivaresapi.b2clogin.com/cristianolivaresapi.onmicrosoft.com/B2C_1_signup_signin/discovery/v2.0/keys";
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        
        converter.setJwtGrantedAuthoritiesConverter(new Converter<Jwt, Collection<GrantedAuthority>>() {
            @Override
            public Collection<GrantedAuthority> convert(Jwt jwt) {
                Collection<GrantedAuthority> authorities = new ArrayList<>();
                
                // Leemos el arreglo de correos del token
                List<String> emails = jwt.getClaimAsStringList("emails");
                String email = (emails != null && !emails.isEmpty()) ? emails.get(0) : "";
                
                // Lógica de perfilamiento real en el backend:
                if ("cristianolivaressandia@gmail.com".equals(email)) {
                    // Tu correo principal obtiene TODOS los privilegios corporativos
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    authorities.add(new SimpleGrantedAuthority("ROLE_DESCARGAR"));
                } else {
                    // Cualquier otro usuario externo registrado solo puede descargar o consultar
                    authorities.add(new SimpleGrantedAuthority("ROLE_DESCARGAR"));
                }
                
                return authorities;
            }
        });
        
        return converter;
    }
}