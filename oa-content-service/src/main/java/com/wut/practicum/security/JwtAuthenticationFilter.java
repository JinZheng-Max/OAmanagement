package com.wut.practicum.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SessionStore sessionStore;

    public JwtAuthenticationFilter(JwtService jwtService, SessionStore sessionStore) {
        this.jwtService = jwtService;
        this.sessionStore = sessionStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                JwtService.UserPrincipal user = jwtService.parse(token);
                boolean active = false;
                try {
                    active = user != null && sessionStore.isActive(user.jti());
                } catch (Exception e) {
                    active = user != null;
                }
                if (active) {
                    String rawRole = user.role() != null ? user.role() : "";
                    String roleName = rawRole.startsWith("ROLE_") ? rawRole.substring(5) : rawRole;
                    var authorities = java.util.List.of(
                        new SimpleGrantedAuthority("ROLE_" + roleName),
                        new SimpleGrantedAuthority(roleName),
                        new SimpleGrantedAuthority(rawRole)
                    );
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception ignored) {
            }
        }
        filterChain.doFilter(request, response);
    }
}
