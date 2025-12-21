package com.example.demo.config;

import com.example.demo.auth.AuthenticationHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
//@EnableAuthorizationServer
public class BrowserSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private AuthenticationHandler authenticationHandler;
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.formLogin() // 表单方式  //.httpBasic() HTTP Basic方式
//                .loginPage("/login.html")
//                .loginProcessingUrl("/login")
                .successHandler(authenticationHandler)
                .failureHandler(authenticationHandler)
                .and()
                .authorizeRequests() // 授权配置
                .antMatchers("/authentication/require"/*, "/login.html"*/).permitAll()
                .anyRequest()  // 所有请求
                .authenticated() // 都需要认证
                .and().csrf().disable()
                .exceptionHandling()
                .accessDeniedHandler(authenticationHandler);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
