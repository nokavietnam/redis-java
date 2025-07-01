package com.hoclamdev.segment;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SegmentedConcurrentLRUMap<K, V> {
    private final Segment<K, V>[] segments;
    private final int segmentCount;

    @SuppressWarnings("unchecked")
    public SegmentedConcurrentLRUMap(int segmentCount, int maxCapacityPerSegment) {
        this.segmentCount = segmentCount;
        this.segments = new Segment[segmentCount];
        for (int i = 0; i < segmentCount; i++) {
            segments[i] = new Segment<>(maxCapacityPerSegment);
        }
    }

    private int getSegmentIndex(Object key) {
        return (key.hashCode() & Integer.MAX_VALUE) % segmentCount;
    }

    public V get(K key) {
        return segmentFor(key).get(key);
    }

    public void put(K key, V value) {
        segmentFor(key).put(key, value);
    }

    public V remove(K key) {
        return segmentFor(key).remove(key);
    }

    public boolean containsKey(K key) {
        return segmentFor(key).containsKey(key);
    }

    public int size() {
        int total = 0;
        for (Segment<K, V> segment : segments) {
            total += segment.size();
        }
        return total;
    }

    private Segment<K, V> segmentFor(K key) {
        return segments[getSegmentIndex(key)];
    }

    public void clear() {
        for (Segment<K, V> segment : segments) {
            segment.clear();
        }
    }

    public void putAll(Map<K, V> m) {
        for (Map.Entry<K, V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> allEntries = new HashSet<>();
        for (Segment<K, V> segment : segments) {
            allEntries.addAll(segment.safeEntrySet());
        }
        return allEntries;
    }

    public Map<K, V> toMap() {
        Map<K, V> result = new LinkedHashMap<>();
        for (Segment<K, V> segment : segments) {
            result.putAll(segment.snapshot());
        }
        return result;
    }
}
