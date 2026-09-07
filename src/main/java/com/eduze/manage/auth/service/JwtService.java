package com.eduze.manage.auth.service;

import com.eduze.manage.common.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public static final String CLAIM_TENANT = "tid";
    public static final String CLAIM_BRANCHES = "bids";
    public static final String CLAIM_AUTHS = "auths";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_TYPE = "typ";
    public static final String CLAIM_TOKEN_VERSION = "tv";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey secretKey;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;

    public JwtService(AppProperties appProperties) {
        String secret = appProperties.getJwt().getSecret();
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        int accessMin = appProperties.getJwt().getAccessTtlMin() != null
                ? appProperties.getJwt().getAccessTtlMin()
                : 15;
        int refreshDay = appProperties.getJwt().getRefreshTtlDay() != null
                ? appProperties.getJwt().getRefreshTtlDay()
                : 7;
        this.accessTtlSeconds = accessMin * 60L;
        this.refreshTtlSeconds = refreshDay * 24L * 3600L;
    }

    public String newJti() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public String generateAccess(
            Long userId,
            String jti,
            Long tenantId,
            int tokenVersion,
            List<Long> branchIds,
            List<String> roles,
            List<String> auths) {
        return buildToken(
                userId, jti, tenantId, tokenVersion, branchIds, roles, auths, TYPE_ACCESS, accessTtlSeconds);
    }

    public String generateRefresh(
            Long userId,
            String jti,
            Long tenantId,
            int tokenVersion,
            List<Long> branchIds,
            List<String> roles,
            List<String> auths) {
        return buildToken(
                userId, jti, tenantId, tokenVersion, branchIds, roles, auths, TYPE_REFRESH, refreshTtlSeconds);
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException ex) {
            throw new JwtException("Invalid JWT", ex);
        }
    }

    public long getAccessTtlSeconds() {
        return accessTtlSeconds;
    }

    public long getRefreshTtlSeconds() {
        return refreshTtlSeconds;
    }

    public Instant getExpiration(Claims claims) {
        return claims.getExpiration().toInstant();
    }

    private String buildToken(
            Long userId,
            String jti,
            Long tenantId,
            int tokenVersion,
            List<Long> branchIds,
            List<String> roles,
            List<String> auths,
            String type,
            long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(jti)
                .subject(String.valueOf(userId))
                .claim(CLAIM_TENANT, tenantId)
                .claim(CLAIM_TOKEN_VERSION, tokenVersion)
                .claim(CLAIM_BRANCHES, branchIds)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_AUTHS, auths)
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(secretKey)
                .compact();
    }
}
