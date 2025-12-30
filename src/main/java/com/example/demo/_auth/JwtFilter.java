package com.example.demo._auth;

import com.example.demo.dto.Result;
import com.example.demo.utils.JsonUtil;
import com.example.demo.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Slf4j
public class JwtFilter extends AuthenticatingFilter {

    @Override
    protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {
//        System.out.println("0 preHandle");
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if (httpRequest.getMethod().equals("OPTIONS")) {
            return true;
        }
        // 检查即将过期的令牌
        String token = httpRequest.getHeader("Authorization");
        if (token != null && JwtUtil.shouldRefresh(token)) {
            String newToken = JwtUtil.refreshToken(token);
            ((HttpServletResponse) response).setHeader("New-Authorization", newToken);
        }
        return super.preHandle(request, response);
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request,
                                     ServletResponse response) throws Exception {
//        System.out.println("1 onAccessDenied");
        return executeLogin(request, response);
    }

    @Override
    protected AuthenticationToken createToken(ServletRequest request,
                                              ServletResponse response) {
//        System.out.println("2 createToken");
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String token = httpRequest.getHeader("Authorization");
        return new JwtToken(token);
    }

    @Override
    protected boolean onLoginFailure(AuthenticationToken token,
                                     AuthenticationException e,
                                     ServletRequest request,
                                     ServletResponse response) {
//        System.out.println("6 onLoginFailure");
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setContentType("application/json;charset=utf-8");

        try (PrintWriter writer = httpResponse.getWriter()) {
            writer.write(JsonUtil.writeValueAsString(
                    Result.failure(401, e.getMessage())
            ));
        } catch (IOException ex) {
            log.error("响应输出失败", ex);
        }
        return false;
    }
}
