package com.hoclamdev.store.datatype.impl;

import com.hoclamdev.store.datatype.RedisDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class RedisZSet extends RedisDataType {
    private final NavigableMap<Double, Set<String>> scoreMap = new ConcurrentSkipListMap<>();

    public RedisZSet(long tll) {
        super(tll);
    }

    @Override
    public String type() { return "zset"; }

    @Override
    public Object get() { return scoreMap; }

    public void zadd(double score, String member) {
        scoreMap.computeIfAbsent(score, k -> ConcurrentHashMap.newKeySet()).add(member);
    }

    public List<String> zrange(int start, int stop) {
        List<String> all = new ArrayList<>();
        for (Set<String> members : scoreMap.values()) all.addAll(members);
        int size = all.size();
        int from = Math.max(0, start < 0 ? size + start : start);
        int to = Math.min(size, stop < 0 ? size + stop + 1 : stop + 1);
        return all.subList(from, Math.max(from, to));
    }

    public Double zscore(String member) {
        for (Map.Entry<Double, Set<String>> entry : scoreMap.entrySet()) {
            if (entry.getValue().contains(member)) return entry.getKey();
        }
        return null;
    }

    public boolean zrem(String member) {
        for (Set<String> members : scoreMap.values()) {
            if (members.remove(member)) return true;
        }
        return false;
    }
}