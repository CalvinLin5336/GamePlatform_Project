package com.example.demo.modules.user.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 取得 Authorization Header
        String authHeader = request.getHeader("Authorization");

        // 沒有 Authorization 或不是 Bearer Token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 去掉 "Bearer "
        String token = authHeader.substring(7);

        try {

            // 驗證 JWT
            if (jwtService.isTokenValid(token)) {

                String account = jwtService.extractAccount(token);
                String role = jwtService.extractRole(token);

                // JWT 必須包含有效的 account 與 role，才建立 Authentication
                if (account == null || account.isBlank() || role == null || role.isBlank()) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                role = role.trim().toUpperCase();

                // user module 目前只允許 PLAYER / ADMIN
                if (!role.equals("PLAYER") && !role.equals("ADMIN")) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                // ROLE_ADMIN / ROLE_PLAYER
                String authority = "ROLE_" + role;

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                account,
                                null,
                                java.util.List.of(
                                        new SimpleGrantedAuthority(authority)
                                )
                        );

                // 放入 Spring Security Context
                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
            }

        } catch (Exception e) {

            // Token 無效時，不建立 Authentication
            SecurityContextHolder.clearContext();
        }

        // 繼續執行後面的 Filter
        filterChain.doFilter(request, response);
    }
}