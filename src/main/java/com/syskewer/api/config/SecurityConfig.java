package com.syskewer.api.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Segurança stateless com JWT.
 * Leitura liberada para autenticados; escrita só para ADMINISTRADOR.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final SecurityFilter securityFilter;

    public SecurityConfig(SecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
    }

    /** @return codificador BCrypt para senhas */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * @param authConfig configuração de autenticação do Spring
     * @return gerenciador de autenticação
     * @throws Exception falha ao obter o bean
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /** Libera front local (dev) com credenciais no header Authorization. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * @param http builder do Spring Security
     * @return cadeia de filtros configurada
     * @throws Exception erro na montagem da cadeia
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // ROTAS PÚBLICAS (Sem token)
                        .requestMatchers(HttpMethod.POST, "/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/password/forgot").permitAll()
                        .requestMatchers(HttpMethod.POST, "/password/reset").permitAll()

                        // SWAGGER
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // REGRAS DE NEGÓCIO
                        .requestMatchers(HttpMethod.POST, "/users/register").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/tables").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/tables/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/categories", "/products", "/prep-locations")
                        .hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PATCH, "/categories/**", "/products/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/categories/**", "/products/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/categories/**", "/products/**").hasRole("ADMINISTRADOR")
                        .anyRequest().authenticated())
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
