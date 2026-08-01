package com.fourguard.wms.infrastructure.security.jwt;

import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.shared.constants.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    public java.util.UUID extractUserId(String token) {
        String userIdStr = extractClaim(token, claims -> claims.get(SecurityConstants.CLAIM_USER_ID, String.class));
        return userIdStr != null ? java.util.UUID.fromString(userIdStr) : null;
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateAccessToken(UserEntity user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put(SecurityConstants.CLAIM_USER_ID, user.getId().toString());
        extraClaims.put(SecurityConstants.CLAIM_EMAIL, user.getEmail());
        extraClaims.put(SecurityConstants.CLAIM_ROLE, user.getRole().getName());
        
        List<String> permissions = user.getRole().getPermissions().stream()
                .map(p -> p.getName())
                .collect(Collectors.toList());
        extraClaims.put(SecurityConstants.CLAIM_PERMISSIONS, permissions);

        return buildToken(extraClaims, user.getUsername(), jwtProperties.getAccessTokenExpiration());
    }

    public String generateRefreshToken(UserEntity user) {
        return buildToken(new HashMap<>(), user.getUsername(), jwtProperties.getRefreshTokenExpiration());
    }

    public boolean isTokenValid(String token, String expectedUsername) {
        final String username = extractUsername(token);
        return (username.equals(expectedUsername)) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
