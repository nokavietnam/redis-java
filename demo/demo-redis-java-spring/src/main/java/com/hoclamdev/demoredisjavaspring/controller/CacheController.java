package com.hoclamdev.demoredisjavaspring.controller;

import com.hoclamdev.demoredisjavaspring.service.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cache")
public class CacheController {

    @Autowired
    private CacheService cacheService;

    @PostMapping
    public String setCache(@RequestParam String key, @RequestParam String value) {
        cacheService.saveToCache(key, value);
        return "Data cached successfully";
    }

    @GetMapping
    public String getCache(@RequestParam String key) {
        String value = cacheService.getFromCache(key);
        return value != null ? value : "No data found for key: " + key;
    }
}
