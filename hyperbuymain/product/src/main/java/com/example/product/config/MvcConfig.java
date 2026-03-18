package com.example.product.config; // Thay đổi package cho phù hợp

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Value("${app.static-resource.public-path-pattern}")
    private String publicPathPattern; // Ví dụ: /product-images/**

    @Value("${app.static-resource.filesystem-location}")
    private String filesystemLocation; // Ví dụ: file:uploads/product-images/ hoặc file:/app/uploads/product-images/

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Đảm bảo filesystemLocation kết thúc bằng dấu "/" nếu nó chưa có
        String location = filesystemLocation.endsWith("/") ? filesystemLocation : filesystemLocation + "/";

        registry.addResourceHandler(publicPathPattern)
                .addResourceLocations(location);
    }
}