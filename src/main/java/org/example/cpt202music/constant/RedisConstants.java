package org.example.cpt202music.constant;

public class RedisConstants {

    // Key prefix for email verification codes in Redis
    public static final String VERIFY_CODE_KEY = "verify:code:";

    // Time-to-live (TTL) for verification codes in minutes
    public static final long VERIFY_CODE_TTL_MINUTES = 5L;

    // Private constructor to prevent instantiation
    private RedisConstants() {}
} 