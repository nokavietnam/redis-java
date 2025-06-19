package com.hoclamdev.protocol.data;

import java.util.List;

public record RedisCommand(String command, List<String> args) {

    public String toCommandString() {
        StringBuilder sb = new StringBuilder(" ");
        sb.append(command);
        for (String arg : args) {
            sb.append(arg);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "RedisCommand{" +
                "command='" + command + '\'' +
                ", args=" + args +
                '}';
    }
}
