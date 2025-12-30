package com.example.demo._auth.aspect;

import com.example.demo._auth.annotation.RequirePermission;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PermissionAspect {
    
    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint,
                                  RequirePermission requirePermission) throws Throwable {
        // 获取当前用户
        Subject subject = SecurityUtils.getSubject();
        // 验证权限
        if (!subject.isPermitted(requirePermission.value())) {
            throw new AuthorizationException("权限不足");
        }
        return joinPoint.proceed();
    }
}