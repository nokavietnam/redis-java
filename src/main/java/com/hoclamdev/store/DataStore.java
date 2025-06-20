package com.hoclamdev.store;

import com.hoclamdev.config.ConfigLoader;
import com.hoclamdev.os.MemoryMonitor;
import com.hoclamdev.store.datatype.RedisDataType;
import com.hoclamdev.store.datatype.impl.RedisHash;
import com.hoclamdev.store.datatype.impl.RedisList;
import com.hoclamdev.store.datatype.impl.RedisSet;
import com.hoclamdev.store.datatype.impl.RedisString;
import com.hoclamdev.store.datatype.impl.RedisZSet;
import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataStore {
    private static final int MAX_KEYS = ConfigLoader.getInt("max.memory.keys", 1000);
    private static final long MAX_MEMORY_LIMIT = ConfigLoader.getInt("max.memory.size", 1024) * 1024L * 1024L;

    // wrap LRUCache Collections.synchronizedMap to make thread safe
    private static final Map<String, RedisDataType> store = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, RedisDataType> eldest) {
                    boolean shouldRemove = size() > MAX_KEYS;
                    if (shouldRemove || MemoryMonitor.isMemoryExceeded(MAX_MEMORY_LIMIT)) {
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

    public synchronized Map<String, RedisDataType> getSnapshot() {
        return new HashMap<>(store);
    }

    public synchronized Map<String, Long> getTTLMapSnapshot() {
        return new HashMap<>(ttlMap);
    }

    public synchronized void loadSnapshot(Map<String, RedisDataType> snapshot, Map<String, Long> ttlSnapshot) {
        store.clear();
        ttlMap.clear();
        store.putAll(snapshot);
        ttlMap.putAll(ttlSnapshot);
    }

    public void set(String key, String value) {
        store.put(key, new RedisString(value));
        ttlMap.remove(key);
    }

    public void setEx(String key, String value, int ttlSeconds) {
        store.put(key, new RedisString(value));
        ttlMap.put(key, System.currentTimeMillis() + ttlSeconds * 1000L);
    }

//    public void setWithTTL(String key, String value, int ttlSeconds) {
//        store.put(key, new RedisString(value));
//        ttlMap.put(key, System.currentTimeMillis() + ttlSeconds * 1000L);
//    }

    public String get(String key) {
        if (isExpired(key)) return null;
        RedisDataType data = store.get(key);
        return data instanceof RedisString ? (String) data.get() : null;
    }

    public int rpush(String key, String value) {
        if (isExpired(key)) return 0;
        RedisList list = (store.containsKey(key) && store.get(key) instanceof RedisList redisList) ?
                redisList : new RedisList();
        list.rpush(value);
        store.put(key, list);
        return list.size();
    }

    public List<String> lrange(String key, int start, int stop) {
        if (isExpired(key)) return Collections.emptyList();
        RedisDataType data = store.get(key);
        return data instanceof RedisList redisList ? redisList.lrange(start, stop) : Collections.emptyList();
    }

    public void sadd(String key, String value) {
        if (isExpired(key)) return;
        RedisSet set = (store.containsKey(key) && store.get(key) instanceof RedisSet redisSet) ?
                redisSet : new RedisSet();
        set.sadd(value);
        store.put(key, set);
    }

    public boolean sismember(String key, String value) {
        if (isExpired(key)) return false;
        RedisDataType data = store.get(key);
        return data instanceof RedisSet redisSet && redisSet.contains(value);
    }

    public void hset(String key, String field, String value) {
        if (isExpired(key)) return;
        RedisHash hash = (store.containsKey(key) && store.get(key) instanceof RedisHash redisHash) ?
                redisHash : new RedisHash();
        hash.hset(field, value);
        store.put(key, hash);
    }

    public String hget(String key, String field) {
        if (isExpired(key)) return null;
        RedisDataType data = store.get(key);
        return data instanceof RedisHash redisHash ? redisHash.hget(field) : null;
    }

    public boolean hdel(String key, String field) {
        if (isExpired(key)) return false;
        RedisDataType data = store.get(key);
        return data instanceof RedisHash redisHash && redisHash.hdel(field);
    }

    public void zadd(String key, double score, String member) {
        if (isExpired(key)) return;
        RedisZSet zset = (store.containsKey(key) && store.get(key) instanceof RedisZSet redisZSet) ?
                redisZSet : new RedisZSet();
        zset.zadd(score, member);
        store.put(key, zset);
    }

    public List<String> zrange(String key, int start, int stop) {
        if (isExpired(key)) return Collections.emptyList();
        RedisDataType data = store.get(key);
        return data instanceof RedisZSet redisZSet ? redisZSet.zrange(start, stop) : Collections.emptyList();
    }

    public boolean zrem(String key, String member) {
        if (isExpired(key)) return false;
        RedisDataType data = store.get(key);
        return data instanceof RedisZSet redisZSet && redisZSet.zrem(member);
    }


    public boolean del(String key) {
        ttlMap.remove(key);
        return store.remove(key) != null;
    }

    // EXPIRE: đặt TTL cho key
    public boolean expire(String key, int seconds) {
        if (!store.containsKey(key)) return false;
        ttlMap.put(key, System.currentTimeMillis() + seconds * 1000L);
        return true;
    }

    // TTL: trả số giây còn lại, hoặc -2 nếu không tồn tại, -1 nếu không có TTL
    public long ttl(String key) {
        Long exp = ttlMap.get(key);
        if (exp == null) return -1;
        long ttl = (exp - System.currentTimeMillis()) / 1000;
        return ttl >= 0 ? ttl : -1;
    }

    private boolean isExpired(String key) {
        Long exp = ttlMap.get(key);
        if (exp != null && System.currentTimeMillis() > exp) {
            store.remove(key);
            ttlMap.remove(key);
            return true;
        }
        return false;
    }

    public Map<String, String> getInfo() {
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
//        long free = osBean.getFreeMemorySize();
//        long used = total - free;
//        info.put("used_memory", String.valueOf(used));
//        info.put("used_memory_human", formatBytes(used));
        info.put("total_system_memory", MemoryMonitor.formatBytes(total));
        info.put("total_system_memory_human", MemoryMonitor.formatBytes(total));

        long used = MemoryMonitor.getUsedMemory();
        long max = MemoryMonitor.getMaxJvmMemory();
        info.put("used_memory", MemoryMonitor.formatBytes(used));
        info.put("used_memory_human", MemoryMonitor.formatBytes(used));
        info.put("max_memory", MemoryMonitor.formatBytes(MAX_MEMORY_LIMIT));
        info.put("max_memory_human", MemoryMonitor.formatBytes(MAX_MEMORY_LIMIT));
        info.put("memory_exceeded", String.valueOf(used >= MAX_MEMORY_LIMIT));
        info.put("max_jvm_memory", MemoryMonitor.formatBytes(max));
        return info;
    }

    public Map<String, String> getMemoryInfo() {
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

    private String formatBytes(long bytes) {
        if (bytes < 1024)
            return bytes + "B";
        int unit = 1024;
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "KMGTPE".charAt(exp - 1) + "i";
        return String.format("%.1f%sb", bytes / Math.pow(unit, exp), pre);
    }

    public void cleanExpiredKeys() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : ttlMap.entrySet()) {
            if (entry.getValue() < now) {
                del(entry.getKey());
            }
        }
    }
}
