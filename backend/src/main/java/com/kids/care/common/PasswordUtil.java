package com.kids.care.common;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 口令摘要工具：PBKDF2-HmacSHA256 + 随机盐（JDK 内置，无外部依赖）。
 * <p>存储格式：{@code pbkdf2$迭代次数$Base64盐$Base64摘要}。
 * 同时兼容历史 MD5 摘要，登录成功后可自动升级为 PBKDF2。</p>
 */
public final class PasswordUtil {

    private static final String PREFIX = "pbkdf2";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_BYTES = 16;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    private PasswordUtil() {
    }

    /** 生成加盐摘要 */
    public static String encode(String raw) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(raw.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        return PREFIX + "$" + ITERATIONS + "$" + ENCODER.encodeToString(salt) + "$" + ENCODER.encodeToString(hash);
    }

    /** 校验明文口令与存储摘要是否匹配（兼容历史 MD5） */
    public static boolean matches(String raw, String encoded) {
        if (raw == null || encoded == null || encoded.isEmpty()) {
            return false;
        }
        if (!needsUpgrade(encoded)) {
            String[] parts = encoded.split("\\$");
            if (parts.length != 4) {
                return false;
            }
            int iterations;
            byte[] salt;
            byte[] expected;
            try {
                iterations = Integer.parseInt(parts[1]);
                salt = DECODER.decode(parts[2]);
                expected = DECODER.decode(parts[3]);
            } catch (IllegalArgumentException e) {
                return false;
            }
            byte[] actual = pbkdf2(raw.toCharArray(), salt, iterations, expected.length * 8);
            return MessageDigest.isEqual(expected, actual);
        }
        // 历史数据：32 位 MD5 摘要
        return Md5Util.matches(raw, encoded);
    }

    /** 是否仍是历史 MD5 摘要（需要升级） */
    public static boolean needsUpgrade(String encoded) {
        return encoded == null || !encoded.startsWith(PREFIX + "$");
    }

    private static byte[] pbkdf2(char[] raw, byte[] salt, int iterations, int keyLength) {
        try {
            PBEKeySpec spec = new PBEKeySpec(raw, salt, iterations, keyLength);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("口令摘要计算失败", e);
        }
    }

    /** 供初始化数据生成摘要：java -cp ... PasswordUtil 明文 */
    public static void main(String[] args) {
        String raw = args.length > 0 ? args[0] : "123456";
        System.out.println(encode(raw));
    }
}
