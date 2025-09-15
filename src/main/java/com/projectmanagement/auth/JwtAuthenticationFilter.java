package com.projectmanagement.auth;

import com.projectmanagement.user.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String LOGIN_ENDPOINT = "/api/auth/login";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_TOKEN_BEGIN_INDEX = 7;

    private final JwtService jwtService;
    private final UserService userService;
    private final JwtBlackListService jwtBlackListService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (shouldSkipAuthentication(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        final String jwt = extractTokenFromHeader(authHeader);
        
        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            authenticateUser(jwt, request);
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkipAuthentication(String servletPath) {
        return LOGIN_ENDPOINT.equals(servletPath);
    }

    private String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authHeader.substring(BEARER_TOKEN_BEGIN_INDEX);
    }

    private void authenticateUser(String jwt, HttpServletRequest request) {
        final String userEmail = jwtService.extractEmail(jwt);
        final String jti = jwtService.extractJti(jwt);

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtBlackListService.isBlacklisted(jti)) {
                log.debug("Authentication rejected: token is blacklisted. JTI={}", jti);
                return;
            }

            UserDetails userDetails = this.userService.loadUserByUsername(userEmail);

            if (jwtService.isTokenValid(jwt, userDetails.getUsername())) {
                String role = jwtService.extractRole(jwt);
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        authorities
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("User authenticated successfully: email={}, role={}", userEmail, role);
            }
        }
    }
}