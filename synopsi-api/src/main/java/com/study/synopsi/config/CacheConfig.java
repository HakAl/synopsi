package com.study.synopsi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(PersonalizationConfig personalizationConfig) {
        // Add all cache names here
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "personalizedFeed",
                "feeds",
                "sources",
                "articles"
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(personalizationConfig.getCache().getMaxCacheSize())
                .expireAfterWrite(personalizationConfig.getCache().getTtlMinutes(), TimeUnit.MINUTES)
                .recordStats());

        return cacheManager;
    }
}