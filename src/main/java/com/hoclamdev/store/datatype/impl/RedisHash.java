package com.hoclamdev.store.datatype.impl;

import com.hoclamdev.store.datatype.RedisDataType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RedisHash extends RedisDataType {
    private final Map<String, String> map = new ConcurrentHashMap<>();

    public RedisHash(long tll) {
        super(tll);
    }

    @Override
    public String type() {
        return "hash";
    }

    @Override
    public Object get() {
        return map;
    }

    public void hset(String field, String value) {
        map.put(field, value);
    }

    public String hget(String field) {
        return map.get(field);
    }

    public boolean hdel(String field) { return map.remove(field) != null; }
}
