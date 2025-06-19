package com.hoclamdev.handler;

import com.hoclamdev.common.SnapshotType;
import com.hoclamdev.config.ConfigLoader;
import com.hoclamdev.encoder.RESPEncoder;
import com.hoclamdev.snapshot.AOFLogger;
import com.hoclamdev.store.DataStore;

import java.util.List;

public class CommandRouter {

    private CommandRouter() {}

    public static String processRESP(List<String> command) {
        String commandType = command.get(0).toUpperCase();
        SnapshotType mode = SnapshotType.fromString(ConfigLoader.get("persistence.mode", "RDB").toUpperCase());
        return switch (commandType) {
            case "PING" -> RESPEncoder.simple("PONG");
            case "SET" -> handleSet(command, mode);
            case "GET" -> handleGet(command);
            case "DEL" -> handleDel(command, mode);
            default -> RESPEncoder.error("ERR unknown command");
        };
    }

    private static String handleSet(List<String> command, SnapshotType mode) {
        if (SnapshotType.AOF.equals(mode)) {
            AOFLogger.logCommand(command);
        }
        if (command.size() == 3) {
            DataStore.set(command.get(1), command.get(2));
            return RESPEncoder.simple("OK");
        } else if (command.size() == 5 && command.get(3).equalsIgnoreCase("EX")) {
            try {
                int ttl = Integer.parseInt(command.get(4));
                DataStore.setWithTTL(command.get(1), command.get(2), ttl);
                return RESPEncoder.simple("OK");
            } catch (NumberFormatException e) {
                return RESPEncoder.error("-ERR invalid TTL");
            }
        } else {
            return RESPEncoder.error("-ERR wrong number of arguments for 'SET'");
        }
    }

    private static String handleGet(List<String> command) {
        String val = DataStore.get(command.get(1));
        return RESPEncoder.bulk(val);
    }

    private static String handleDel(List<String> command, SnapshotType mode) {
        if (SnapshotType.AOF.equals(mode)) {
            AOFLogger.logCommand(command);
        }
        boolean removed = DataStore.del(command.get(1));
        return RESPEncoder.integer(removed ? 1 : 0);
    }
}
