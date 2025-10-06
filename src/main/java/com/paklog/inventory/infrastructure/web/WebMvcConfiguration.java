package com.paklog.inventory.infrastructure.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for API versioning and deprecation handling.
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final DeprecationInterceptor deprecationInterceptor;

    public WebMvcConfiguration(DeprecationInterceptor deprecationInterceptor) {
        this.deprecationInterceptor = deprecationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register deprecation interceptor for all API endpoints
        registry.addInterceptor(deprecationInterceptor)
                .addPathPatterns("/inventory/**", "/api/**");
    }
}
