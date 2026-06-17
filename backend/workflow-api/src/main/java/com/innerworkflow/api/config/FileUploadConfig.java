package com.innerworkflow.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    @Value("${workflow.file.upload-path:./uploads/}")
    private String uploadPath;

    @Value("${workflow.file.url-prefix:/static/}")
    private String urlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = uploadPath.endsWith("/") ? uploadPath : uploadPath + "/";
        String pattern = urlPrefix.startsWith("/") ? urlPrefix : "/" + urlPrefix;
        pattern = pattern.endsWith("/") ? pattern + "**" : pattern + "/**";
        registry.addResourceHandler(pattern)
                .addResourceLocations("file:" + path);
    }
}
