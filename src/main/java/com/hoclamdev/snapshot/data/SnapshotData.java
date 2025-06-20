package com.hoclamdev.snapshot.data;

import com.hoclamdev.store.datatype.RedisDataType;

import java.io.Serializable;
import java.util.Map;

public class SnapshotData implements Serializable {
    private final Map<String, RedisDataType> store;
    private final Map<String, Long> ttlMap;

    public SnapshotData(Map<String, RedisDataType> store, Map<String, Long> ttlMap) {
        this.store = store;
        this.ttlMap = ttlMap;
    }

    public Map<String, RedisDataType> getStore() {
        return store;
    }

    public Map<String, Long> getTtlMap() {
        return ttlMap;
    }
}
