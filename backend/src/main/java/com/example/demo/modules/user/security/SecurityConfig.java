package com.example.demo.modules.user.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // User API 啟用 CORS，讓前端 Live Server 可以呼叫 User API
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // REST API 不使用 CSRF
            .csrf(csrf -> csrf.disable())

            // 主畫面以同來源 iframe 載入 Lobby、User、Board 等頁面。
            // 仍禁止外部網站嵌入，避免 clickjacking。
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
            )

            // JWT 不使用 Session
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // API 權限
            .authorizeHttpRequests(auth -> auth

                // User 登入 API
                .requestMatchers("/api/user/auth/**").permitAll()

                // Board 登入 / 註冊 API
                .requestMatchers("/api/auth/**").permitAll()

                // Admin API 必須先放在 /api/** 前面
                .requestMatchers("/api/user/admin/**").hasRole("ADMIN")

                // Player 個人資料 API
                .requestMatchers("/api/user/player/**").hasAnyRole("PLAYER","ADMIN")

                // 開發階段暫時允許其他 API
                .requestMatchers("/**").permitAll()

                // 其他請求仍需要登入
                .anyRequest().authenticated()
            )

            // JWT Filter
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
