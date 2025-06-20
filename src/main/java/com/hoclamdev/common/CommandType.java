package com.hoclamdev.common;

import java.util.HashMap;
import java.util.Map;

public enum CommandType {
    PING,
    SET,
    GET,
    DEL,
    INFO,
    EXPIRE,
    TTL,
    RPUSH,
    LRANGE,
    SADD,
    SISMEMBER,
    HSET,
    HGET,
    HDEL,
    ZADD,
    ZRANGE,
    ZSCORE,
    ZREM,
    UNKNOWN;

    private static final Map<String, CommandType> lookup = new HashMap<>();

    static {
        lookup.put("PING", PING);
        lookup.put("SET", SET);
        lookup.put("GET", GET);
        lookup.put("DEL", DEL);
        lookup.put("INFO", INFO);
        lookup.put("EXPIRE", EXPIRE);
        lookup.put("TTL", TTL);
        lookup.put("RPUSH", RPUSH);
        lookup.put("LRANGE", LRANGE);
        lookup.put("SADD", SADD);
        lookup.put("SISMEMBER", SISMEMBER);
        lookup.put("HSET", HSET);
        lookup.put("HGET", HGET);
        lookup.put("HDEL", HDEL);
        lookup.put("ZADD", ZADD);
        lookup.put("ZRANGE", ZRANGE);
        lookup.put("ZSCORE", ZSCORE);
        lookup.put("ZREM", ZREM);
    }

    public static CommandType fromString(String type) {
        return lookup.getOrDefault(type, UNKNOWN);
    }
}
