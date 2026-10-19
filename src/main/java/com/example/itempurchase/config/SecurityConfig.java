package com.example.itempurchase.config;

import com.example.itempurchase.security.AppUserDetailsService;
import com.example.itempurchase.security.JwtAuthFilter;
import com.example.itempurchase.security.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtUtil jwtUtil,
                                                   AppUserDetailsService userDetailsService) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)                 // not needed: no cookies, token in header
            .cors(cors -> { })                                      // uses the corsConfigurationSource bean
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) ->
                        writeError(res, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required. Please log in"))
                .accessDeniedHandler((req, res, ex) ->
                        writeError(res, HttpServletResponse.SC_FORBIDDEN,
                                "You do not have permission to perform this action")))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                // reading is allowed for every logged-in user
                .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                // creating / changing / deleting item types and items: ADMIN only
                .requestMatchers("/api/item-types/**", "/api/items/**").hasRole("ADMIN")
                // purchases: ADMIN and USER can create and update (there is NO delete endpoint at all)
                .requestMatchers(HttpMethod.POST, "/api/purchases/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.PUT, "/api/purchases/**").hasAnyRole("ADMIN", "USER")
                .anyRequest().authenticated())
            .addFilterBefore(new JwtAuthFilter(jwtUtil, userDetailsService),
                    UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }

    private static void writeError(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.getWriter().write("{\"success\":false,\"status\":" + status + ",\"message\":\"" + message + "\"}");
    }
}
