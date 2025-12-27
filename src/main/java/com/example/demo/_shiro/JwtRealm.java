package com.example.demo._shiro;

import com.example.demo.entity.Users;
import com.example.demo.service.UserService;
import com.example.demo.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;


public class JwtRealm extends AuthorizingRealm {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean supports(AuthenticationToken token) {
//        System.out.println("3 supports");
        return token instanceof JwtToken;
    }

    // 认证
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token)
            throws AuthenticationException {
//        System.out.println("4 doGetAuthenticationInfo");
        JwtToken jwtToken = (JwtToken) token;
        String jwt = (String) jwtToken.getCredentials();

        try {
            Claims claims = JwtUtil.parseToken(jwt);
            String username = claims.getSubject();

//            // 检查Redis中令牌是否失效
//            if (RedisUtils.isTokenBlacklisted(jwt)) {
//                throw new ExpiredCredentialsException("token已失效");
//            }
            Users users = userService.getUser(username);
            if (users == null) {
                throw new AuthenticationException("用户名或密码错误");
            }
            if (users.getStatus() == 0) {
                throw new AuthenticationException("用户被禁用");
            }

            return new SimpleAuthenticationInfo(username, jwt, getName());
        } catch (Exception e) {
            throw new AuthenticationException("无效token");
        }
    }

    // 授权
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
//        System.out.println("5 doGetAuthorizationInfo");
        String username = (String) principals.getPrimaryPrincipal();
        String cacheKey = "shiro:auth:" + username;
        // 从数据库或缓存获取用户角色权限
        AuthorizationInfo authorizationInfo = (AuthorizationInfo) redisTemplate.opsForValue().get(cacheKey);
        if (authorizationInfo != null) {
            return authorizationInfo;
        }
        SimpleAuthorizationInfo simpleAuthorizationInfo = new SimpleAuthorizationInfo();
        simpleAuthorizationInfo.setRoles(userService.getUserRoles(username));
        simpleAuthorizationInfo.setStringPermissions(userService.getUserPermissions(username));
        redisTemplate.opsForValue().set(cacheKey, simpleAuthorizationInfo, 1, TimeUnit.DAYS);
        return simpleAuthorizationInfo;
    }
}
