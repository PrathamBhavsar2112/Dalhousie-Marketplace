package com.dalhousie.dalhousie_marketplace_backend.Config;

import com.dalhousie.dalhousie_marketplace_backend.service.AuthService;
import com.dalhousie.dalhousie_marketplace_backend.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtil jwtUtil, AuthService authService) {
        return new JwtAuthenticationFilter(jwtUtil, authService);
    }
}