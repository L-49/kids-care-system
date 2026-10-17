package com.kids.care.common;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * MD5 工具类（教学演示用；生产环境应使用 BCrypt 等加盐算法）
 */
public final class Md5Util {

    private Md5Util() {
    }

    public static String md5(String raw) {
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean matches(String raw, String encoded) {
        return encoded != null && encoded.equalsIgnoreCase(md5(raw));
    }
}
