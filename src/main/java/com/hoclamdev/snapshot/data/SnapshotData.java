package com.hoclamdev.snapshot.data;

import java.io.Serializable;
import java.util.Map;

public record SnapshotData(Map<String, String> store, Map<String, Long> ttlMap) implements Serializable {
}
