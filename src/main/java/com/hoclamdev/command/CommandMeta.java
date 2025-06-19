package com.hoclamdev.command;

import java.util.List;

public class CommandMeta {
    public final String name;
    public final long arity;
    public final List<String> flags;
    public final long firstKey;
    public final long lastKey;
    public final long step;

    public CommandMeta(String name, long arity, List<String> flags, long firstKey, long lastKey, long step) {
        this.name = name;
        this.arity = arity;
        this.flags = flags;
        this.firstKey = firstKey;
        this.lastKey = lastKey;
        this.step = step;
    }

    public List<Object> toRESP3Array() {
        return List.of(name, arity, flags, firstKey, lastKey, step);
    }
}
