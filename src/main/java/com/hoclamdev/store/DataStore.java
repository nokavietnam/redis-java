package com.hoclamdev.store;

import com.hoclamdev.config.ConfigLoader;
import com.hoclamdev.os.MemoryMonitor;
import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataStore {
    private static final int MAX_KEYS = ConfigLoader.getInt("max.memory.keys", 1000);
    private static final long maxMemoryLimit = ConfigLoader.getInt("max.memory.size", 1024) * 1024L * 1024L;

    // wrap LRUCache Collections.synchronizedMap to make thread safe
    private static final Map<String, String> store = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    boolean shouldRemove = size() > MAX_KEYS;
                    if (shouldRemove || MemoryMonitor.isMemoryExceeded(maxMemoryLimit)) {
                        ttlMap.remove(eldest.getKey()); // xóa TTL nếu bị đẩy ra bởi LRU
                    }
                    return shouldRemove;
                }
            }
    );
    private static final Map<String, Long> ttlMap = new ConcurrentHashMap<>();

    private DataStore() {}

    private static class Holder {
        private static final DataStore INSTANCE = new DataStore();
    }

    public static DataStore getInstance() {
        return Holder.INSTANCE;
    }

    public Map<String, Long> getTTLMap() {
        return ttlMap;
    }

    public synchronized Map<String, String> getSnapshot() {
        return new HashMap<>(store);
    }

    public synchronized Map<String, Long> getTTLMapSnapshot() {
        return new HashMap<>(ttlMap);
    }

    public synchronized void loadSnapshot(Map<String, String> snapshot, Map<String, Long> ttlSnapshot) {
        store.clear();
        ttlMap.clear();
        store.putAll(snapshot);
        ttlMap.putAll(ttlSnapshot);
    }

    public void set(String key, String value) {
        store.put(key, value);
        ttlMap.remove(key);
    }

    public void setWithTTL(String key, String value, int ttlSeconds) {
        store.put(key, value);
        ttlMap.put(key, System.currentTimeMillis() + ttlSeconds * 1000L);
    }

    public String get(String key) {
        Long expireAt = ttlMap.get(key);
        if (expireAt != null && expireAt < System.currentTimeMillis()) {
            store.remove(key);
            ttlMap.remove(key);
            return null;
        }
        return store.get(key);
    }

    public boolean del(String key) {
        ttlMap.remove(key);
        return store.remove(key) != null;
    }

    // EXPIRE: đặt TTL cho key
    public static boolean expire(String key, int seconds) {
        if (!store.containsKey(key)) return false;
        ttlMap.put(key, System.currentTimeMillis() + seconds * 1000L);
        return true;
    }

    // TTL: trả số giây còn lại, hoặc -2 nếu không tồn tại, -1 nếu không có TTL
    public static long ttl(String key) {
        if (!store.containsKey(key)) return -2;
        Long expireAt = ttlMap.get(key);
        if (expireAt == null) return -1;
        long ttlMillis = expireAt - System.currentTimeMillis();
        return ttlMillis > 0 ? ttlMillis / 1000 : -2;
    }


    public static Map<String, String> getInfo() {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        int port = ConfigLoader.getInt("server.port", 6379);
        Map<String, String> info = new LinkedHashMap<>();
        info.put("redis_version", ConfigLoader.get("version", "1.0.0"));
        info.put("tcp_port", String.valueOf(port));
        info.put("process_id", String.valueOf(ProcessHandle.current().pid()));
        info.put("keys", String.valueOf(store.size()));
        info.put("ttl_keys", String.valueOf(ttlMap.size()));
        info.put("max_keys", String.valueOf(MAX_KEYS));

        long total = osBean.getTotalMemorySize();
        long free = osBean.getFreeMemorySize();
//        long used = total - free;
//        info.put("used_memory", String.valueOf(used));
//        info.put("used_memory_human", formatBytes(used));
        info.put("total_system_memory", MemoryMonitor.formatBytes(total));
        info.put("total_system_memory_human", MemoryMonitor.formatBytes(total));

        long used = MemoryMonitor.getUsedMemory();
        long max = MemoryMonitor.getMaxJvmMemory();
        info.put("used_memory", MemoryMonitor.formatBytes(used));
        info.put("used_memory_human", MemoryMonitor.formatBytes(used));
        info.put("max_memory", MemoryMonitor.formatBytes(maxMemoryLimit));
        info.put("max_memory_human", MemoryMonitor.formatBytes(maxMemoryLimit));
        info.put("memory_exceeded", String.valueOf(used >= maxMemoryLimit));
        info.put("max_jvm_memory", MemoryMonitor.formatBytes(max));
        return info;
    }

    public static Map<String, String> getMemoryInfo() {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        long total = osBean.getTotalMemorySize();
        long free = osBean.getFreeMemorySize();
        long used = total - free;

        Map<String, String> mem = new LinkedHashMap<>();
        mem.put("used_memory", String.valueOf(used));
        mem.put("used_memory_human", formatBytes(used));
        mem.put("total_system_memory", String.valueOf(total));
        mem.put("total_system_memory_human", formatBytes(total));
        return mem;
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024)
            return bytes + "B";
        int unit = 1024;
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "KMGTPE".charAt(exp - 1) + "i";
        return String.format("%.1f%sb", bytes / Math.pow(unit, exp), pre);
    }
}
