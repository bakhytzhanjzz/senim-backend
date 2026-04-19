package com.senim.backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configures a RedisCacheManager with per-cache TTLs.
     * Cached DTOs implement java.io.Serializable to satisfy JDK serialization.
     *
     * <pre>
     *   dashboard     – 5 minutes  (aggregated agency dashboard per agencyId)
     *   notifications – 60 seconds (unread notification list per userId)
     * </pre>
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues();

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("dashboard",
                        defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("notifications",
                        defaultConfig.entryTtl(Duration.ofSeconds(60)))
                .build();
    }
}
