//package com.example.fooddonation.config;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
//public class CorsConfig implements WebMvcConfigurer {
//
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**")
//                .allowedOrigins(
//                    "http://localhost:3000",
//                    "https://*.ngrok-free.app"          // allows all ngrok urls
//                    // OR put exact one: "https://random-1234-abc.ngrok-free.app"
//                )
//                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
//                .allowedHeaders("*")
//                .allowCredentials(true);
//    }
//}
