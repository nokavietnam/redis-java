package com.hoclamdev.protocol.data;

import java.util.List;

public record RedisCommand(String command, List<String> args) {

    public String toCommandString() {
        StringBuilder sb = new StringBuilder();
        sb.append(command);
        sb.append(" ");
        for (String arg : args) {
            sb.append(arg);
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    @Override
    public String toString() {
        return "RedisCommand{" +
                "command='" + command + '\'' +
                ", args=" + args +
                '}';
    }
}
