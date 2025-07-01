package com.hoclamdev.segment;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

class Segment<K, V> {
    private final int maxCapacity;
    private final LinkedHashMap<K, V> map;
    private final ReentrantLock lock = new ReentrantLock();

    Segment(int maxCapacity) {
        this.maxCapacity = maxCapacity;
        this.map = new LinkedHashMap<>(16, 0.75f, true);
    }

    V get(K key) {
        lock.lock();
        try {
            return map.get(key);
        } finally {
            lock.unlock();
        }
    }

    void put(K key, V value) {
        lock.lock();
        try {
            map.put(key, value);
            if (map.size() > maxCapacity) {
                Iterator<K> it = map.keySet().iterator();
                if (it.hasNext()) {
                    K eldest = it.next();
                    it.remove();
                    // System.out.println("Evicted: " + eldest);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    V remove(K key) {
        lock.lock();
        try {
            return map.remove(key);
        } finally {
            lock.unlock();
        }
    }

    boolean containsKey(K key) {
        lock.lock();
        try {
            return map.containsKey(key);
        } finally {
            lock.unlock();
        }
    }

    int size() {
        lock.lock();
        try {
            return map.size();
        } finally {
            lock.unlock();
        }
    }

    void clear() {
        lock.lock();
        try {
            map.clear();
        } finally {
            lock.unlock();
        }
    }

    Set<Map.Entry<K, V>> safeEntrySet() {
        lock.lock();
        try {
            // return copy prevent ConcurrentModificationException
            return new HashSet<>(map.entrySet());
        } finally {
            lock.unlock();
        }
    }

    Map<K, V> snapshot() {
        lock.lock();
        try {
            return new LinkedHashMap<>(map); // return copy to thread-safe
        } finally {
            lock.unlock();
        }
    }
}
