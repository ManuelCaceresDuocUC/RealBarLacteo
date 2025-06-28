package com.whatsappbot.whatsappservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.info("🔧 Aplicando configuración CORS personalizada...");
        registry.addMapping("/**")
            .allowedOrigins(
                "http://localhost:3000", 
                "https://barlacteo-catalogo.s3.us-east-1.amazonaws.com",
                "https://realbarlacteo-1.onrender.com" ,
                "https://fronted-autoservicio.vercel.app",
                    "https://fronted-autoservicio-krfmxq7zg-manuel-caceres-projects.vercel.app" // ✅ AGREGA ESTA
 // ✅ AGREGA ESTE
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE","PATCH", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(false);
    }
}