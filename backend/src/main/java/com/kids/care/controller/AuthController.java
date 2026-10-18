package com.kids.care.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kids.care.common.BizException;
import com.kids.care.common.Md5Util;
import com.kids.care.common.Result;
import com.kids.care.common.TokenStore;
import com.kids.care.entity.SysUser;
import com.kids.care.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录认证接口
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserMapper sysUserMapper;
    private final TokenStore tokenStore;

    /** 登录请求体 */
    public record LoginBody(String username, String password) {}

    /** 登录：校验账号密码，签发令牌 */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginBody body) {
        if (body == null || !StringUtils.hasText(body.username()) || !StringUtils.hasText(body.password())) {
            throw new BizException("请输入账号和密码");
        }
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, body.username().trim()));
        if (user == null || !Md5Util.matches(body.password(), user.getPassword())) {
            throw new BizException("账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException("账号已被停用，请联系管理员");
        }
        TokenStore.LoginUser loginUser = new TokenStore.LoginUser(
                user.getId(), user.getUsername(), user.getRealName(), user.getRole(), user.getChildId());
        String token = tokenStore.create(loginUser);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        return Result.ok(data);
    }

    /** 获取当前登录人信息 */
    @GetMapping("/info")
    public Result<TokenStore.LoginUser> info(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return Result.ok(tokenStore.get(token));
    }

    /** 退出登录 */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        tokenStore.remove(token);
        return Result.ok();
    }
}
