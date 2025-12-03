package com.tms.backend.jwt;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.tms.backend.user.CustomUserDetails;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secret;

    @Autowired
    private JwtService jwtService;

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Skip JWT processing for OPTIONS requests (CORS preflight)
        if ("OPTIONS".equals(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String uid;
        final Long id;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7); // remove "Bearer "
        uid = jwtService.extractUid(jwt);
        id = jwtService.extractId(jwt);

        if (uid != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            String role = jwtService.extractRole(jwt);
            List<GrantedAuthority> authority = List.of(
                    new SimpleGrantedAuthority(role) // e.g., "administrator"
            );
            // you can also load more user details from DB if needed
            CustomUserDetails userDetails = new CustomUserDetails(
                    id,
                    uid,
                    jwtService.extractUsername(jwt),
                    "", // no password from token
                    authority // authorities if included in JWT
            );

            log.info("JWT Filter - User: {}, Authorities: {}", 
                    userDetails.getUsername(),
                    userDetails.getAuthorities());

            if (jwtService.isTokenValid(jwt, userDetails)) {
                log.info("✅ Token valid, setting authentication");
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
