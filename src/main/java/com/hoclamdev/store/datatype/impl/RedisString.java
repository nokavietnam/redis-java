package com.hoclamdev.store.datatype.impl;

import com.hoclamdev.store.datatype.RedisDataType;

public class RedisString extends RedisDataType {
    private final String value;

    public RedisString(String value) {
        super(-1L);
        this.value = value;
    }

    public RedisString(String value, long tll) {
        super(tll);
        this.value = value;
    }
    @Override
    public String type() {
        return "string";
    }

    @Override
    public Object get() {
        return value;
    }
}
