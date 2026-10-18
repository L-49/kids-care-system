package com.kids.care.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kids.care.common.BizException;
import com.kids.care.common.PasswordUtil;
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
    public Result<Map<String, Object>> login(@RequestBody LoginBody body, HttpServletRequest request) {
        if (body == null || !StringUtils.hasText(body.username()) || !StringUtils.hasText(body.password())) {
            throw new BizException("请输入账号和密码");
        }
        // 登录失败也要记录真实操作人，供操作日志使用
        request.setAttribute("logUsername", body.username().trim());
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, body.username().trim()));
        if (user == null || !PasswordUtil.matches(body.password(), user.getPassword())) {
            throw new BizException("账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException("账号已被停用，请联系管理员");
        }
        upgradePasswordIfNeeded(user, body.password());
        request.setAttribute("logUsername", user.getRealName());
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

    /** 历史 MD5 口令在登录成功后自动升级为 PBKDF2 加盐摘要 */
    private void upgradePasswordIfNeeded(SysUser user, String rawPassword) {
        if (!PasswordUtil.needsUpgrade(user.getPassword())) {
            return;
        }
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(PasswordUtil.encode(rawPassword));
        sysUserMapper.updateById(update);
        user.setPassword(update.getPassword());
    }
}
