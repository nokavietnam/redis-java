package com.hoclamdev.store.datatype.impl;

import com.hoclamdev.store.datatype.RedisDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RedisList extends RedisDataType {
    private final List<String> list = Collections.synchronizedList(new ArrayList<>());

    public RedisList(long tll) {
        super(tll);
    }

    @Override
    public String type() {
        return "list";
    }

    @Override
    public Object get() {
        return list;
    }

    public int size() {
        return list.size();
    }

    public void rpush(String value) {
        list.add(value);
    }

    public List<String> lrange(int start, int stop) {
        int size = list.size();
        int from = Math.max(0, start < 0 ? size + start : start);
        int to = Math.min(size, stop < 0 ? size + stop + 1 : stop + 1);
        return list.subList(from, Math.max(from, to));
    }
}
