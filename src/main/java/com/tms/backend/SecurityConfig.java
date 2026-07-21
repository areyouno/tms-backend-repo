package com.tms.backend;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.backend.auth.CustomUserDetailsService;
import com.tms.backend.jwt.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;



@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Autowired
    private CustomUserDetailsService userDetailsService;

    /**
     * Removes the default "ROLE_" prefix used by Spring Security for roles.
     * This makes hasRole('administrator') check for the authority 'administrator',
     * matching the non-prefixed authorities from the database.
     */
    @Bean
    public GrantedAuthorityDefaults grantedAuthorityDefaults() {
        return new GrantedAuthorityDefaults(""); // Set the prefix to an empty string
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Tells the frontend why a request was unauthenticated. When JwtAuthenticationFilter
     * flags the request as "jwt_expired", the response distinguishes that from a missing/invalid
     * token so the frontend can auto-logout specifically on expiry.
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            boolean expired = Boolean.TRUE.equals(request.getAttribute("jwt_expired"));

            log.warn("401 Unauthorized - {} {} - reason: {}", request.getMethod(), request.getRequestURI(),
                    expired ? "token_expired" : "unauthorized");

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");

            ErrorResponse errorResponse = new ErrorResponse(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    expired ? "token_expired" : "unauthorized",
                    expired ? "Session expired. Please log in again." : "Authentication required.",
                    request.getRequestURI()
            );

            new ObjectMapper().writeValue(response.getWriter(), errorResponse);
        };
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authenticationProvider(authenticationProvider()) // Add this line!
                .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/users/register").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/verify").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/verifyUserCreated").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users/set-password").permitAll()
                .requestMatchers(HttpMethod.POST,"/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/jobs/upload-translated").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/jobAnalysis/sizing-sse/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/tm/import-tmx/jobs/*/stream").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/termbase/import/jobs/*/stream").permitAll()
                .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "https://xliffl10n.latispass.net"
        ));
        
        config.setAllowedMethods(List.of("GET","POST","PUT", "PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}


