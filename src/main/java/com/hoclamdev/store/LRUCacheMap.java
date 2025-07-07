package com.hoclamdev.store;

import com.hoclamdev.os.MemoryMonitor;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCacheMap<K, V> extends LinkedHashMap<K, V> {

    private final int maxSize;
    private final long maxMem;

    public LRUCacheMap(int maxSize, long maxMem) {
        super(maxSize, 0.75f, true);
        this.maxSize = maxSize;
        this.maxMem = maxMem;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return (size() > maxSize || MemoryMonitor.isMemoryExceeded(maxMem));
    }
}
