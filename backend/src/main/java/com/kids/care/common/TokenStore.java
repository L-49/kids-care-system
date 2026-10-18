package com.kids.care.common;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简易令牌存储（教学演示用，生产环境应使用 JWT + Redis）
 */
@Component
public class TokenStore {

    /**
     * 登录人信息
     *
     * @param childId 家长账号绑定的幼儿ID，非家长角色为 null
     */
    public record LoginUser(Long userId, String username, String realName, String role, Long childId) {}

    private final Map<String, LoginUser> tokens = new ConcurrentHashMap<>();

    public String create(LoginUser user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, user);
        return token;
    }

    public LoginUser get(String token) {
        return token == null ? null : tokens.get(token);
    }

    public void remove(String token) {
        tokens.remove(token);
    }
}
