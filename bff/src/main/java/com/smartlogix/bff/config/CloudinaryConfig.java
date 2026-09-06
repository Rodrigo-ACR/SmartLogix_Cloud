package com.smartlogix.bff.config;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(Map.of(
                "cloud_name", "diq24kgrd",
                "api_key", "992133557747379",
                "api_secret", "4g58zQQ1gr6ozjQbWYLKBFi3buA"
        ));
    }
}