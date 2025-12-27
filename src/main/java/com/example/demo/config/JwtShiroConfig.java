package com.example.demo.config;

import com.example.demo.auth.JwtFilter;
import com.example.demo.auth.JwtRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class JwtShiroConfig {

    @Bean
    public Realm jwtRealm() {
        JwtRealm jwtRealm = new JwtRealm();
        jwtRealm.setCachingEnabled(false);
        jwtRealm.setAuthenticationCachingEnabled(false);
        jwtRealm.setAuthorizationCachingEnabled(false);
        return jwtRealm;
    }

    @Bean
    public SessionManager sessionManager() {
        DefaultWebSessionManager manager = new DefaultWebSessionManager();
        manager.setSessionValidationSchedulerEnabled(false); // 强制每次验证
        manager.setSessionIdCookieEnabled(false); // 使用JWT不需要Cookie
        return manager;
    }

    @Bean
    public DefaultWebSecurityManager securityManager() {
        DefaultWebSecurityManager manager = new DefaultWebSecurityManager();
        manager.setRealm(jwtRealm());
        manager.setSessionManager(sessionManager());
        manager.setCacheManager(null);
        manager.setRememberMeManager(null); // 禁用RememberMe
        return manager;
    }

    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(DefaultWebSecurityManager securityManager) {
        ShiroFilterFactoryBean factory = new ShiroFilterFactoryBean();
        factory.setSecurityManager(securityManager);

        // 自定义过滤器
        Map<String, Filter> filters = new HashMap<>();
        filters.put("jwt", new JwtFilter());
        factory.setFilters(filters);

        // 拦截规则
        Map<String, String> filterChain = new LinkedHashMap<>();
        // 登录接口放行
        filterChain.put("/test/login", "anon");
        // swagger放行
        filterChain.put("/swagger-ui.html", "anon");
        filterChain.put("/webjars/**", "anon");
        filterChain.put("/swagger-resources/**", "anon");
        filterChain.put("/v2/api-docs", "anon");
        filterChain.put("/csrf", "anon");
        filterChain.put("/v3/api-docs", "anon");
        filterChain.put("/doc.html", "anon");
        // 其他请求需JWT验证
        filterChain.put("/**", "jwt");
        factory.setFilterChainDefinitionMap(filterChain);

        return factory;
    }
}
