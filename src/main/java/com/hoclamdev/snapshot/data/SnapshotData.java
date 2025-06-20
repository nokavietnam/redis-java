package com.hoclamdev.snapshot.data;

import com.hoclamdev.store.datatype.RedisDataType;

import java.io.Serializable;
import java.util.Map;

public class SnapshotData implements Serializable {
    private final Map<String, RedisDataType> store;

    public SnapshotData(Map<String, RedisDataType> store) {
        this.store = store;
    }

    public Map<String, RedisDataType> getStore() {
        return store;
    }
}
