package com.innerworkflow.common.util;

import cn.hutool.core.util.IdUtil;
import com.innerworkflow.common.exception.BusinessException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.jackson.io.JacksonDeserializer;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * <p>
 * 基于jjwt 0.12.x版本，提供Token的生成、解析、验证功能
 * </p>
 *
 * @author InnerWorkflow Team
 * @since 1.0.0
 */
@Slf4j
public class JwtUtils {

    /**
     * 默认密钥（Base64编码，至少256位用于HS256）
     */
    private static final String DEFAULT_SECRET = "aW5uZXItd29ya2Zsb3ctaHViLXNlY3JldC1rZXktZm9yLWp3dC1zaWduaW5n";

    /**
     * 默认Token有效期：7天（毫秒）
     */
    private static final long DEFAULT_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L;

    /**
     * Token前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 默认密钥对象
     */
    private static final SecretKey DEFAULT_KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(DEFAULT_SECRET));

    /**
     * 使用默认密钥和有效期生成Token
     *
     * @param claims Token中包含的自定义数据
     */
    public static String generateToken(Map<String, Object> claims) {
        return generateToken(claims, DEFAULT_SECRET, DEFAULT_EXPIRE_TIME);
    }

    /**
     * 生成Token（自定义密钥和有效期）
     *
     * @param claims     Token中包含的自定义数据
     * @param secret     签名密钥（Base64编码）
     * @param expireTime 过期时间（毫秒）
     */
    public static String generateToken(Map<String, Object> claims, String secret, long expireTime) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        long now = System.currentTimeMillis();

        // 添加jti作为唯一标识，用于Token黑名单等场景
        if (!claims.containsKey("jti")) {
            claims = new HashMap<>(claims);
            claims.put("jti", IdUtil.fastSimpleUUID());
        }

        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireTime))
                .signWith(key)
                .compact();
    }

    /**
     * 解析Token（使用默认密钥）
     *
     * @param token JWT Token字符串
     * @return Token中的Claims数据
     * @throws BusinessException Token无效时抛出401异常
     */
    public static Claims parseToken(String token) {
        return parseToken(token, DEFAULT_SECRET);
    }

    /**
     * 解析Token（自定义密钥）
     *
     * @param token  JWT Token字符串
     * @param secret 签名密钥（Base64编码）
     * @return Token中的Claims数据
     * @throws BusinessException Token无效时抛出401异常
     */
    public static Claims parseToken(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));

        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .json(new JacksonDeserializer<>(Map.of()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", e.getMessage());
            throw BusinessException.paramError("Token已过期，请重新登录");
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的Token格式: {}", e.getMessage());
            throw BusinessException.paramError("Token格式不正确");
        } catch (MalformedJwtException e) {
            log.warn("Token格式错误: {}", e.getMessage());
            throw BusinessException.paramError("Token格式错误");
        } catch (SignatureException e) {
            log.warn("Token签名验证失败: {}", e.getMessage());
            throw BusinessException.paramError("Token签名无效");
        } catch (IllegalArgumentException e) {
            log.warn("Token为空: {}", e.getMessage());
            throw BusinessException.paramError("Token不能为空");
        }
    }

    /**
     * 验证Token是否有效（使用默认密钥）
     *
     * @param token JWT Token字符串
     * @return true-有效，false-无效
     */
    public static boolean validateToken(String token) {
        return validateToken(token, DEFAULT_SECRET);
    }

    /**
     * 验证Token是否有效（自定义密钥）
     *
     * @param token  JWT Token字符串
     * @param secret 签名密钥（Base64编码）
     * @return true-有效，false-无效
     */
    public static boolean validateToken(String token, String secret) {
        try {
            parseToken(token, secret);
            return true;
        } catch (Exception e) {
            log.debug("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从Token中获取指定值（使用默认密钥）
     */
    public static Object getClaim(String token, String claimKey) {
        return parseToken(token).get(claimKey);
    }

    /**
     * 从Token中获取用户ID（使用默认密钥）
     */
    public static Long getUserId(String token) {
        Object userId = getClaim(token, "userId");
        return userId != null ? Long.valueOf(userId.toString()) : null;
    }

    /**
     * 从Token中获取用户名（使用默认密钥）
     */
    public static String getUsername(String token) {
        Object username = getClaim(token, "username");
        return username != null ? username.toString() : null;
    }

    /**
     * 获取Token剩余有效时间（毫秒）
     */
    public static long getRemainingTime(String token) {
        Claims claims = parseToken(token);
        Date expiration = claims.getExpiration();
        return expiration != null
                ? Math.max(0, expiration.getTime() - System.currentTimeMillis())
                : 0;
    }

    /**
     * 移除Bearer前缀
     */
    public static String removeBearerPrefix(String token) {
        if (token != null && token.startsWith(TOKEN_PREFIX)) {
            return token.substring(TOKEN_PREFIX.length());
        }
        return token;
    }

    /**
     * 从指定字符串生成密钥（非Base64，适用于自定义明文密钥场景）
     */
    public static String encodeSecret(String rawSecret) {
        byte[] bytes = rawSecret.getBytes(StandardCharsets.UTF_8);
        // 确保至少32字节（256位）
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            bytes = padded;
        }
        return java.util.Base64.getEncoder().encodeToString(bytes);
    }
}
