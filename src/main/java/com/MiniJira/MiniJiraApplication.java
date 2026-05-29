package com.minijira;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

// ENABLE SPRING'S ANNOTATION-DRIVEN CACHING - REQUIRED FOR @Cacheable, @CacheEvict TO WORK
@EnableCaching
@SpringBootApplication
public class MiniJiraApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniJiraApplication.class, args);
    }
}
