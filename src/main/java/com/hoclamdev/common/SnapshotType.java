package com.hoclamdev.common;

import java.util.HashMap;
import java.util.Map;

public enum SnapshotType {
    UNKNOWN,
    AOF,
    RDB;

    private static final Map<String, SnapshotType> lookup = new HashMap<>();

    static {
        lookup.put("AOF", AOF);
        lookup.put("RDB", RDB);
    }

    public static SnapshotType fromString(String type) {
        return lookup.getOrDefault(type, UNKNOWN);
    }
}
