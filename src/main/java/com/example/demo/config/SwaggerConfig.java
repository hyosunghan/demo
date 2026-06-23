package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.Parameter;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableSwagger2
public class SwaggerConfig {
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                // 每个接口需单独设置认证信息
//                .globalOperationParameters(parameters())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.example.demo.controller"))
                .paths(PathSelectors.any())
                .build()
                // 全局只设置一次认证信息
                .securitySchemes(Collections.singletonList(new ApiKey("Authorization", "Authorization", "header")))
                .securityContexts(Collections.singletonList(SecurityContext.builder().securityReferences(defaultAuth()).build()))
                .pathMapping("/");
    }

    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "every api");
        return Collections.singletonList(new SecurityReference("Authorization", new AuthorizationScope[]{authorizationScope}));
    }

    private List<Parameter> parameters() {
        ParameterBuilder parameterBuilder = new ParameterBuilder();
        List<Parameter> parameters = new ArrayList<>();
        // 配置全局 token 参数
        parameterBuilder.name("Authorization")
                .description("认证 token")
                .modelRef(new ModelRef("string"))
                .parameterType("header")
                .required(false) // 根据需要设置是否必需
                .build();
        parameters.add(parameterBuilder.build());
        return parameters;
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Spring Boot 集成 Swagger")
                .description("API 文档")
                .version("1.0")
                .build();
    }
}