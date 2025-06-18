package com.hoclamdev.snapshot.data;

import java.io.Serializable;
import java.util.Map;

public class SnapshotData implements Serializable {
    private final Map<String, String> store;
    private final Map<String, Long> ttlMap;

    public SnapshotData(Map<String, String> store, Map<String, Long> ttlMap) {
        this.store = store;
        this.ttlMap = ttlMap;
    }

    public Map<String, String> getStore() {
        return store;
    }

    public Map<String, Long> getTtlMap() {
        return ttlMap;
    }
}
