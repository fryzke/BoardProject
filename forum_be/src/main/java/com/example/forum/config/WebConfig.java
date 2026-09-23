package com.example.forum.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.forum.interceptor.RateLimitInterceptor;

import lombok.RequiredArgsConstructor;

import java.io.File;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File uploadFolder = new File(uploadDir);
        String absolutePath = uploadFolder.getAbsolutePath();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath + "/");
        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}
