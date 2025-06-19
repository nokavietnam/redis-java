package com.hoclamdev.command;

import java.util.List;

public record CommandMeta(String name, long arity, List<String> flags, long firstKey, long lastKey, long step) {

    public List<Object> toRESP3Array() {
        return List.of(name, arity, flags, firstKey, lastKey, step);
    }
}
