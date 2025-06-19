package com.hoclamdev.handler;

import com.hoclamdev.command.CommandRegistry;
import com.hoclamdev.common.SnapshotType;
import com.hoclamdev.config.ConfigLoader;
import com.hoclamdev.encoder.RESPEncoder;
import com.hoclamdev.protocol.data.RedisCommand;
import com.hoclamdev.snapshot.AOFLogger;
import com.hoclamdev.store.DataStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class CommandRouter {

    private static final Logger log = LogManager.getLogger(CommandRouter.class);

    private CommandRouter() {}

    public static String processRESP(RedisCommand command) {
        if (command == null) {
            return RESPEncoder.error("Empty command");
        }

        log.info("=========> " + command.getCommand() + " : " + command.getArgs());

        String commandType = command.getCommand().toUpperCase();
        SnapshotType mode = SnapshotType.fromString(ConfigLoader.get("persistence.mode", "RDB").toUpperCase());
        return switch (commandType) {
            case "PING" -> handlePing(command);
            case "SET" -> handleSet(command, mode);
            case "GET" -> handleGet(command);
            case "DEL" -> handleDel(command, mode);
            case "INFO" -> handleInfo();
            case "EXPIRE" -> handleExpire(command);
            case "TTL" -> handleTTL(command);
//            case "COMMAND" -> handleCommand();
            default -> RESPEncoder.error("ERR unknown command");
        };
    }

    private static String handleCommand() {
        List<List<Object>> allCommands = CommandRegistry.toRESP3ArrayList();
        return RESPEncoder.arrayOfArrays(allCommands);
    }

    private static String handlePing(RedisCommand command) {
        String msg = !command.getArgs().isEmpty() ? command.getArgs().get(0) : "PONG";
        return RESPEncoder.verbatim("txt", msg);
    }

    private static String handleSet(RedisCommand command, SnapshotType mode) {
        if (command.getArgs().size() < 2) {
            return RESPEncoder.error("ERR wrong number of arguments for 'SET'");
        }
        if (SnapshotType.AOF.equals(mode)) {
            AOFLogger.logCommand(command.toCommandString());
        }
        if (command.getArgs().size() == 2) {
            DataStore.getInstance().set(command.getArgs().get(0), command.getArgs().get(1));
            return RESPEncoder.simple("OK");
        } else if (command.getArgs().size() == 4 && command.getArgs().get(2).equalsIgnoreCase("EX")) {
            try {
                int ttl = Integer.parseInt(command.getArgs().get(3));
                DataStore.getInstance().setWithTTL(command.getArgs().get(0), command.getArgs().get(1), ttl);
                return RESPEncoder.simple("OK");
            } catch (NumberFormatException e) {
                return RESPEncoder.error("-ERR invalid TTL");
            }
        } else {
            return RESPEncoder.error("-ERR wrong number of arguments for 'SET'");
        }
    }

    private static String handleGet(RedisCommand command) {
        if (command.getArgs().size() != 1) {
            return RESPEncoder.error("ERR wrong number of arguments for 'GET'");
        }
        String val = DataStore.getInstance().get(command.getArgs().get(0));
        return val == null ? RESPEncoder.nullString() : RESPEncoder.bulk(val);
    }

    private static String handleDel(RedisCommand command, SnapshotType mode) {
        if (command.getArgs().isEmpty()) {
            return RESPEncoder.error("ERR wrong number of arguments for 'DEL'");
        }
        if (SnapshotType.AOF.equals(mode)) {
            AOFLogger.logCommand(command.toCommandString());
        }
        int count = 0;
        for (int i = 0; i < command.getArgs().size(); i++) {
            if (DataStore.getInstance().del(command.getArgs().get(i))) {
                count++;
            }
        }
        return RESPEncoder.integer(count);
    }

    private static String handleExpire(RedisCommand command) {
        if (command.getArgs().size() != 2) {
            return RESPEncoder.error("ERR wrong number of arguments for 'EXPIRE'");
        }
        String key = command.getArgs().get(0);
        int ttl = Integer.parseInt(command.getArgs().get(1));
        boolean ok = DataStore.expire(key, ttl);
        return RESPEncoder.bool(ok);
    }

    private static String handleTTL(RedisCommand command) {
        if (command.getArgs().size() != 1) {
            return RESPEncoder.error("ERR wrong number of arguments for 'TTL'");
        }
        String key = command.getArgs().get(0);
        long ttl = DataStore.ttl(key);
        return ttl >= 0 ? RESPEncoder.integer(ttl) : RESPEncoder.nullString();
    }

    private static String handleInfo() {
        Map<String, String> info = DataStore.getInfo();
        return RESPEncoder.convertInfoMapToResp("SERVER", info);
    }


}
