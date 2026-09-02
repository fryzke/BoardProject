package com.example.forum.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads/images}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File uploadFolder = new File(uploadDir);
        String absolutePath = uploadFolder.getAbsolutePath();

        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}
