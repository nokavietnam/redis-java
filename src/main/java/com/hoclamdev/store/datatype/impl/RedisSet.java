package com.hoclamdev.store.datatype.impl;

import com.hoclamdev.store.datatype.RedisDataType;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RedisSet extends RedisDataType {
    private final Set<String> set = ConcurrentHashMap.newKeySet();

    public RedisSet(long tll) {
        super(tll);
    }

    @Override
    public String type() {
        return "set";
    }

    @Override
    public Object get() {
        return set;
    }

    public void sadd(String value) {
        set.add(value);
    }

    public boolean contains(String value) {
        return set.contains(value);
    }
}
