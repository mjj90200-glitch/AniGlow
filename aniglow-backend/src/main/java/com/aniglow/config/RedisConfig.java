package com.aniglow.config;

import com.aniglow.dto.anime.AnimeDto;
import com.aniglow.dto.anime.AnimeListResponse;
import com.aniglow.dto.community.CommunityDto;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
@Slf4j
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用 String 序列化器作为 key
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // 使用 JSON 序列化器作为 value
        GenericJackson2JsonRedisSerializer jsonSerializer = jsonSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper,
            @Value("${aniglow.cache.key-prefix:aniglow::}") String keyPrefix
    ) {
        GenericJackson2JsonRedisSerializer serializer = jsonSerializer();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .computePrefixWith(cacheName -> keyPrefix + cacheName + "::")
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues();

        RedisCacheConfiguration animeListConfig = typed(config, objectMapper,
                objectMapper.constructType(AnimeListResponse.class));
        RedisCacheConfiguration animeDetailConfig = typed(config, objectMapper,
                objectMapper.constructType(AnimeDto.class));
        RedisCacheConfiguration animeSeasonConfig = typed(config, objectMapper,
                objectMapper.getTypeFactory().constructCollectionType(List.class, AnimeDto.class));
        RedisCacheConfiguration communityListConfig = typed(config, objectMapper,
                objectMapper.getTypeFactory().constructCollectionType(List.class, CommunityDto.class));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withCacheConfiguration("animeList", animeListConfig.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("animeDetail", animeDetailConfig.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("animeSeason", animeSeasonConfig.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("ranking", animeListConfig.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("communityList", communityListConfig.entryTtl(Duration.ofMinutes(5)))
                .transactionAware()
                .build();
    }

    /** Redis 短暂不可用时回源数据库，缓存不能成为网站单点故障。 */
    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("缓存读取失败，已回源数据库: cache={}, key={}, reason={}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("缓存写入失败，当前请求不受影响: cache={}, key={}, reason={}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("缓存失效失败: cache={}, key={}", cache.getName(), key);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("缓存清理失败: cache={}", cache.getName());
            }
        };
    }

    @Bean
    public CachingConfigurer cachingConfigurer(CacheManager cacheManager, CacheErrorHandler cacheErrorHandler) {
        return new CachingConfigurer() {
            @Override
            public CacheManager cacheManager() {
                return cacheManager;
            }

            @Override
            public CacheErrorHandler errorHandler() {
                return cacheErrorHandler;
            }
        };
    }

    private GenericJackson2JsonRedisSerializer jsonSerializer() {
        return new GenericJackson2JsonRedisSerializer().configure(mapper -> {
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        });
    }

    private RedisCacheConfiguration typed(
            RedisCacheConfiguration base,
            ObjectMapper objectMapper,
            JavaType javaType
    ) {
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, javaType);
        return base.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }
}
