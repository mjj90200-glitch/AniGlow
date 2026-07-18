package com.aniglow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnimeCacheInvalidator {

    private final CacheManager cacheManager;

    public void invalidateReadModels() {
        clear("animeList");
        clear("animeDetail");
        clear("animeSeason");
        clear("ranking");
    }

    private void clear(String name) {
        Cache cache = cacheManager.getCache(name);
        if (cache != null) cache.clear();
    }
}
