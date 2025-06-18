package com.hoclamdev.handler;

import com.hoclamdev.config.ConfigLoader;
import com.hoclamdev.snapshot.AOFLogger;
import com.hoclamdev.store.DataStore;

import java.util.List;

public class GetSetDelHandler implements CommandHandler {
    @Override
    public String handle(List<String> command) {
        String cmd = command.get(0).toUpperCase();
        return switch (cmd) {
            case "SET" -> handleSet(command);
            case "GET" -> handleGet(command);
            case "DEL" -> handleDel(command);
            default -> "-ERR unknown command\r\n";
        };
    }

    private String handleSet(List<String> command) {
        String mode = ConfigLoader.get("persistence.mode", "RDB").toUpperCase();
        if (mode.equals("AOF")) {
            AOFLogger.logCommand(command);
        }
        if (command.size() == 3) {
            DataStore.set(command.get(1), command.get(2));
            return "+OK\r\n";
        } else if (command.size() == 5 && command.get(3).equalsIgnoreCase("EX")) {
            try {
                int ttl = Integer.parseInt(command.get(4));
                DataStore.setWithTTL(command.get(1), command.get(2), ttl);
                return "+OK\r\n";
            } catch (NumberFormatException e) {
                return "-ERR invalid TTL\r\n";
            }
        } else {
            return "-ERR wrong number of arguments for 'SET'\r\n";
        }
    }

    private String handleGet(List<String> command) {
        String val = DataStore.get(command.get(1));
        return val != null ? "$" + val.length() + "\r\n" + val + "\r\n" : "$-1\r\n";
    }

    private String handleDel(List<String> command) {
        boolean deleted = DataStore.del(command.get(1));
        return  ":" + (deleted ? "1" : "0") + "\r\n";
    }
}
