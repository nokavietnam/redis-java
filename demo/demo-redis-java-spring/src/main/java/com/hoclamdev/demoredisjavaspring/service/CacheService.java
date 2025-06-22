package com.hoclamdev.demoredisjavaspring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    @Autowired
    public StringRedisTemplate redisTemplate;

    public void saveToCache(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public String getFromCache(String key) {
        return redisTemplate.opsForValue().get(key);
    }
}
