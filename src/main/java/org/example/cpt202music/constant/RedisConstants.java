package org.example.cpt202music.constant;

public class RedisConstants {

    // Key prefix for email verification codes in Redis
    public static final String VERIFY_CODE_KEY = "verify:code:";

    // Time-to-live (TTL) for verification codes in minutes
    public static final long VERIFY_CODE_TTL_MINUTES = 5L;

    /**
     * 注册用户信息在Redis中的key前缀
     */
    public static final String REGISTER_USER_KEY = "register:user:";

    /**
     * 注册用户信息在Redis中的过期时间（分钟）
     */
    public static final long REGISTER_USER_TTL_MINUTES = 10;

    // Private constructor to prevent instantiation
    private RedisConstants() {}
} 