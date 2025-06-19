package com.hoclamdev.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CommandRegistry {
    private static final Map<String, CommandMeta> commandMap = new LinkedHashMap<>();

    static {
        register("ping", 1, List.of("fast", "readonly"), 0, 0, 0);
        register("set", -3, List.of("write", "denyoom"), 1, 1, 1);
        register("get", 2, List.of("readonly", "fast"), 1, 1, 1);
        register("del", -2, List.of("write"), 1, -1, 1);
        register("expire", 3, List.of("write"), 1, 1, 1);
        register("ttl", 2, List.of("readonly", "fast"), 1, 1, 1);
        register("info", -1, List.of("admin", "fast", "noscript"), 0, 0, 0);
        register("command", 1, List.of("admin", "loading", "stale"), 0, 0, 0);
    }

    private static void register(String name, long arity, List<String> flags, long firstKey, long lastKey, long step) {
        commandMap.put(name.toUpperCase(), new CommandMeta(name, arity, flags, firstKey, lastKey, step));
    }

    public static CommandMeta get(String name) {
        return commandMap.get(name.toUpperCase());
    }

    public static Collection<CommandMeta> getAll() {
        return commandMap.values();
    }

    public static List<List<Object>> toRESP3ArrayList() {
        List<List<Object>> result = new ArrayList<>();
        for (CommandMeta meta : commandMap.values()) {
            result.add(meta.toRESP3Array());
        }
        return result;
    }
}
