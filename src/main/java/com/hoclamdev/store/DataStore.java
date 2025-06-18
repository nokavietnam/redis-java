package com.hoclamdev.store;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataStore {

    private static volatile DataStore instance;
    private static final Map<String, String> store = new ConcurrentHashMap<>();
    private static final Map<String, Long> ttlMap = new ConcurrentHashMap<>();

    private DataStore() {}

    public static DataStore getInstance() {
        if (instance == null) {
            synchronized (DataStore.class) {
                if (instance == null) {
                    instance = new DataStore();
                }
            }
        }
        return instance;
    }

    public static Map<String, Long> getTTLMap() {
        return ttlMap;
    }

    public static synchronized Map<String, String> getSnapshot() {
        return new HashMap<>(store);
    }

    public static synchronized Map<String, Long> getTTLMapSnapshot() {
        return new HashMap<>(ttlMap);
    }

    public static synchronized void loadSnapshot(Map<String, String> snapshot, Map<String, Long> ttlSnapshot) {
        store.clear();
        ttlMap.clear();
        store.putAll(snapshot);
        ttlMap.putAll(ttlSnapshot);
    }

    public static void set(String key, String value){
        store.put(key, value);
        ttlMap.remove(key);
    }

    public static void setWithTTL(String key, String value, int ttlSeconds) {
        store.put(key, value);
        ttlMap.put(key, System.currentTimeMillis() + ttlSeconds * 1000L);
    }

    public static String get(String key) {
        Long expireAt = ttlMap.get(key);
        if (expireAt != null && expireAt < System.currentTimeMillis()) {
            store.remove(key);
            ttlMap.remove(key);
            return null;
        }
        return store.get(key);
    }

    public static boolean del(String key) {
        ttlMap.remove(key);
        return store.remove(key) != null;
    }
}
