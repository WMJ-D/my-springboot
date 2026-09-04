package com.example.test.security;

import com.example.test.common.AppException;
import com.example.test.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 工具，与 Express 侧 jsonwebtoken 签发的 payload 保持一致：
 * { sub: 用户ID, username, roles: [...], jti: 会话ID }
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expiresInSeconds;

    public JwtTokenProvider(AppProperties appProperties) {
        String secret = appProperties.getJwt().getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret 至少需要 32 个字符");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiresInSeconds = appProperties.getJwt().getExpiresInSeconds();
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public String createToken(long userId, String username, List<String> roles, String sessionId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("roles", roles)
                .id(sessionId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expiresInSeconds)))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验令牌，失败抛出 AppException(401)
     */
    public CurrentUser parse(String token) {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (Exception error) {
            throw new AppException(401, "登录凭证无效或已过期", "INVALID_TOKEN");
        }
        String jti = claims.getId();
        if (jti == null || jti.isBlank()) {
            throw new AppException(401, "登录会话无效，请重新登录", "INVALID_SESSION");
        }
        long userId;
        try {
            userId = Long.parseLong(claims.getSubject());
        } catch (NumberFormatException error) {
            throw new AppException(401, "登录凭证无效或已过期", "INVALID_TOKEN");
        }
        Object usernameClaim = claims.get("username");
        Object rolesClaim = claims.get("roles");
        List<String> roles = rolesClaim instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();
        return new CurrentUser(userId, usernameClaim == null ? "" : String.valueOf(usernameClaim), roles, jti);
    }
}
