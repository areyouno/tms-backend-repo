package com.tms.backend.jwt;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.tms.backend.user.CustomUserDetails;
import com.tms.backend.user.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationMs;

    public String extractEmail(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secret.getBytes(StandardCharsets.UTF_8))
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public Long extractId(String token) {
        return extractClaim(token, claims -> claims.get("id", Long.class));
    }

    public String extractUid(String token) {
        return extractClaim(token, claims -> claims.get("uid", String.class));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secret.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token, CustomUserDetails userDetails) {
        final String uid = extractUid(token);
        return (uid.equals(userDetails.getUid())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String generateToken(User user) {
        System.out.println("Generating JWT for user - Email: " + user.getEmail() + ", UID: '" + user.getUid() + "'");

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("uid", user.getUid());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().getName());
        // claims.put("isVerified", user.isVerified());
        // claims.put("isProfileComplete", user.isProfileComplete());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        // Convert secret to a proper key
        Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail()) // subject can be email or uid
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key)
                .compact();
    }

}
