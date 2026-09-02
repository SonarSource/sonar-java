package com.example.modulea;

import com.example.common.CacheProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheProvider redisCacheProvider() {
        return key -> "redis:" + key;
    }

    @Bean
    public CacheProvider inMemoryCacheProvider() {
        return key -> "memory:" + key;
    }

    @Bean
    public CacheProvider diskCacheProvider() {
        return key -> "disk:" + key;
    }
}
