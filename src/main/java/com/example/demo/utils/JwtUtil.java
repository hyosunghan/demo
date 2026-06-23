package com.example.demo.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class JwtUtil {
    private static final String SECRET_KEY = "pL2xD5dF8zP4jU5vB4nR6pZ8rH0zS1iC9pE7uZ6yV6lD3xM9dU0yG1sT8eQ8jS9uO";
    public static final long EXPIRATION = 86400000L; // 24小时

    // 生成令牌
    public static String generateToken(String username, Set<String> roles, Set<String> permissions) {
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    // 解析令牌
    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 验证令牌
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean shouldRefresh(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            long remainingTime = expiration.getTime() - System.currentTimeMillis();
            // 当剩余时间少于30分钟时建议刷新
            return remainingTime < 1800000L; // 30分钟 = 30 * 60 * 1000ms
        } catch (Exception e) {
            return false;
        }
    }

    public static String refreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            String username = claims.getSubject();
            Set<String> roles = (Set<String>) claims.get("roles");
            Set<String> permissions = (Set<String>) claims.get("permissions");
            // 生成新的 Token
            return generateToken(username, roles, permissions);
        } catch (Exception e) {
            return null;
        }
    }
}
