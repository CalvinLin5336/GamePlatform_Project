package com.example.demo.modules.user.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private String allowedOriginPatterns;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // 正式部署採同源；只有設定檔明確列出的開發來源可以跨域。
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

            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )

            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            // API 權限
            .authorizeHttpRequests(auth -> auth

                // 瀏覽器預檢與公開靜態資源
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/", "/index.html", "/favicon.ico", "/error",
                        "/assets/**", "/pages/**").permitAll()

                // 原生 WebSocket 無法設定 Authorization Header；握手來源由各 WebSocket
                // Config 限制，連線後的身分驗證仍由各 Handler 負責。
                .requestMatchers("/ws/**").permitAll()

                // 只有登入與註冊可匿名；/me 必須帶有效 JWT。
                .requestMatchers(HttpMethod.POST,
                        "/api/user/auth/login", "/api/user/auth/register").permitAll()

                // 遊戲型錄可供首頁及登入前畫面讀取。
                .requestMatchers(HttpMethod.GET, "/api/game-management/games/**").permitAll()

                // Admin API 必須先放在一般 API 規則前面。
                .requestMatchers("/api/user/admin/**", "/api/admin/**").hasRole("ADMIN")

                // Player 個人資料 API
                .requestMatchers("/api/user/player/**").hasAnyRole("PLAYER","ADMIN")

                // 平台 API 與 Board 功能必須登入。
                .requestMatchers("/api/**", "/board/**").authenticated()

                // 未列入契約的路徑不對外開放。
                .anyRequest().denyAll()
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
        configuration.setAllowedOriginPatterns(Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With",
                "X-Player-Token"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
