// config/WebConfig.java
package com.example.zadel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/")
                .setCachePeriod(3600);
        
        System.out.println("=== WEB CONFIG ===");
        System.out.println("Upload directory: " + uploadDir);
        System.out.println("Static resources mapped: /uploads/** -> file:" + uploadDir + "/");
        System.out.println("=== END WEB CONFIG ===");
    }
}