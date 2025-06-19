package com.hoclamdev.protocol.data;

import java.util.List;

public class RedisCommand {
    private final String command;
    private final List<String> args;

    public RedisCommand(String command, List<String> args) {
        this.command = command;
        this.args = args;
    }

    public String getCommand() {
        return command;
    }

    public List<String> getArgs() {
        return args;
    }

    public String toCommandString() {
        StringBuilder sb = new StringBuilder (" ");
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
