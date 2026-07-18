package com.backend.storage_service.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);
    private final JwtServices jwtServices;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtFilter(JwtServices jwtServices, CustomUserDetailsService customUserDetailsService) {
        this.jwtServices = jwtServices;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException
    {
        String authHeader = request.getHeader("Authorization");
        logger.debug("Processing request: {} {}, Authorization header: {}", request.getMethod(), request.getRequestURI(), authHeader != null ? "present" : "missing");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.debug("No valid Authorization header, skipping JWT processing");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            String username = jwtServices.extractUsername(token);
            logger.debug("Extracted username from token: {}", username);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                logger.debug("Loaded user details for username: {}", username);

                if (jwtServices.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("Successfully authenticated user: {}", username);

                    String userId = jwtServices.extractUserId(token);
                    if (userId != null) {
                        request.setAttribute("userId", UUID.fromString(userId));
                        logger.debug("Set userId attribute: {}", userId);
                    }
                } else {
                    logger.warn("Token validation failed for user: {}", username);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing JWT token", e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
