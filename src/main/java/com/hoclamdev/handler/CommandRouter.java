package com.hoclamdev.handler;

import com.hoclamdev.common.CommandType;
import com.hoclamdev.common.SnapshotType;
import com.hoclamdev.config.ConfigLoader;
import com.hoclamdev.encoder.RESPEncoder;
import com.hoclamdev.protocol.data.RedisCommand;
import com.hoclamdev.snapshot.AOFLogger;
import com.hoclamdev.store.DataStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class CommandRouter {

    private static final Logger log = LogManager.getLogger(CommandRouter.class);

    private CommandRouter() {
    }

    private static class Holder {
        private static final CommandRouter INSTANCE = new CommandRouter();
    }

    public static CommandRouter getInstance() {
        return Holder.INSTANCE;
    }

    public String processRESP(RedisCommand command) {
        if (command == null) {
            return RESPEncoder.error("Empty command");
        }

        log.info("=========> {} : {}", command.command(), command.args());

        String commandType = command.command().toUpperCase();
        SnapshotType mode = SnapshotType.fromString(ConfigLoader.get("persistence.mode", "RDB").toUpperCase());
        return switch (CommandType.fromString(commandType)) {
            case PING -> handlePing(command);
            case HELLO -> handleHello(command);
            case SET -> handleSet(command, mode);
            case GET -> handleGet(command);
            case DEL -> handleDel(command, mode);
            case INFO -> handleInfo();
            case EXPIRE -> handleExpire(command);
            case TTL -> handleTTL(command);
            // LIST
            case RPUSH -> handleRPush(command, mode);
            case LRANGE -> handleLRange(command);
            // SET
            case SADD -> handleSAdd(command, mode);
            case SISMEMBER -> handleSIsMember(command);
            // HASH
            case HSET -> handleHSet(command, mode);
            case HGET -> handleHGet(command);
            case HDEL -> handleHDelete(command, mode);
            // ZSET
            case ZADD -> handleZAdd(command, mode);
            case ZRANGE -> handleZRange(command);
            case ZSCORE -> handleZScore(command);
            case ZREM -> handleZRem(command);
            default -> RESPEncoder.error("ERR unknown command");
        };
    }

    private String handleZRem(RedisCommand command) {
        if (command.args().size() < 3) {
            return RESPEncoder.error("ERR wrong number of arguments for 'ZRem'");
        }
        boolean data = DataStore.getInstance().zrem(command.args().get(0), command.args().get(1));
        return RESPEncoder.integer(data ? 1 : 0);
    }

    private String handleZScore(RedisCommand command) {
        if (command.args().size() < 2) {
            return RESPEncoder.error("ERR wrong number of arguments for 'ZScore'");
        }
        Double data = DataStore.getInstance().zscore(command.args().get(0), command.args().get(1));
        return data == null ? RESPEncoder.nullString() : RESPEncoder.doubleValue(data);
    }

    private String handleZRange(RedisCommand command) {
        if (command.args().size() < 3) {
            return RESPEncoder.error("ERR wrong number of arguments for 'ZRange'");
        }
        List<String> data = DataStore.getInstance().zrange(command.args().get(0),
                        Integer.parseInt(command.args().get(1)),
                        Integer.parseInt(command.args().get(2)));
        return RESPEncoder.array(data);
    }

    private String handleZAdd(RedisCommand command, SnapshotType mode) {
        if (command.args().size() < 3) {
            return RESPEncoder.error("ERR wrong number of arguments for 'ZAdd'");
        }
        if (SnapshotType.AOF.equals(mode)) {
            AOFLogger.logCommand(command.toCommandString());
        }
        DataStore.getInstance().zadd(command.args().get(0), Double.parseDouble(command.args().get(1)), command.args().get(2));
        return RESPEncoder.simple("OK");
    }

    private String handleHDelete(RedisCommand command, SnapshotType mode) {
        if (command.args().size() < 2) {
            return RESPEncoder.error("ERR wrong number of arguments for 'HDelete'");
        }
        if (SnapshotType.AOF.equals(mode)) {
            AOFLogger.logCommand(command.toCommandString());
        }
        boolean data = DataStore.getInstance().hdel(command.args().get(0), command.args().get(1));
        return RESPEncoder.integer(data ? 1 : 0);
    }

    private String handleHGet(RedisCommand command) {
        if (command.args().size() < 2) {
            return RESPEncoder.error("ERR wrong number of arguments for 'HGet'");
        }
        String data = DataStore.getInstance().hget(command.args().get(0), command.args().get(1));
        return data == null ? RESPEncoder.nullString() : RESPEncoder.bulk(data);
    }

    private String handleHello(RedisCommand command) {
        if (command.args().size() >= 3 && command.args().get(1).equals("AUTH")) {
            String username = command.args().get(2);
            String password = command.args().get(3);
            log.info("Authenticating user '{}' with password '{}'", username, password);
        }

        Map<String, Object> helloMap = new LinkedHashMap<>();
        helloMap.put("server", "redis");
        helloMap.put("version", "6.0.0");
        helloMap.put("proto", 3);
        helloMap.put("id", 10);
        helloMap.put("mode", "standalone");
        helloMap.put("role", "master");
        helloMap.put("modules", Collections.emptyList());

        return RESPEncoder.mapToRESP3(helloMap);
    }

    private String handleHSet(RedisCommand command, SnapshotType mode) {
        List<String> args = command.args();
        if (args.size() < 3) {
            return RESPEncoder.error("ERR wrong number of arguments for 'HSET'");
        }
        if (SnapshotType.AOF.equals(mode)) {
            AOFLogger.logCommand(command.toCommandString());
        }
        DataStore.getInstance().hset(args.get(0), args.get(1), args.get(2));
        return RESPEncoder.simple("OK");
    }

    private String handleSIsMember(RedisCommand command) {
        List<String> args = command.args();
        if (args.size() < 2) {
            return RESPEncoder.error("ERR wrong number of arguments for 'SISMEMBER'");
        }
        return RESPEncoder.integer(DataStore.getInstance().sismember(args.get(0), args.get(1)) ? 1 : 0);
    }

    private String handleSAdd(RedisCommand command, SnapshotType mode) {
        List<String> args = command.args();
        if (args.size() < 2) {
            return RESPEncoder.error("ERR wrong number of arguments for 'SADD'");
        }
        if (SnapshotType.AOF.equals(mode)) {
            AOFLogger.logCommand(command.toCommandString());
        }
        DataStore.getInstance().sadd(args.get(0), args.get(1));
        return RESPEncoder.simple("OK");
    }

    private String handleLRange(RedisCommand command) {
        if (command.args().size() < 3) {
            return RESPEncoder.error("ERR wrong number of arguments for 'LRANGE'");
        }
        List<String> args = command.args();
        List<String> data = DataStore.getInstance()
                .lrange(args.get(0), Integer.parseInt(args.get(1)), Integer.parseInt(args.get(2)));
        return RESPEncoder.array(data);
    }

    private String handleRPush(RedisCommand command, SnapshotType mode) {
        if (command.args().size() < 2) {
            return RESPEncoder.error("ERR wrong number of arguments for 'RPUSH'");
        }
        if (SnapshotType.AOF.equals(mode)) {
            AOFLogger.logCommand(command.toCommandString());
        }
        List<String> args = command.args();
        int size = DataStore.getInstance().rpush(args.get(0), args.get(1));
        return RESPEncoder.integer(size);
    }

    private String handlePing(RedisCommand command) {
        String msg = !command.args().isEmpty() ? command.args() .getFirst() : "PONG";
        return RESPEncoder.simple(msg);
    }

    private String handleSet(RedisCommand command, SnapshotType mode) {
        if (command.args().size() < 2) {
            return RESPEncoder.error("ERR wrong number of arguments for 'SET'");
        }
        if (SnapshotType.AOF.equals(mode)) {
            AOFLogger.logCommand(command.toCommandString());
        }
        if (command.args().size() == 2) {
            DataStore.getInstance().set(command.args().get(0), command.args().get(1));
            return RESPEncoder.simple("OK");
        } else if (command.args().size() == 4 && command.args().get(2).equalsIgnoreCase("EX")) {
            try {
                int ttl = Integer.parseInt(command.args().get(3));
                DataStore.getInstance().setEx(command.args().get(0), command.args().get(1), ttl);
                return RESPEncoder.simple("OK");
            } catch (NumberFormatException e) {
                return RESPEncoder.error("-ERR invalid TTL");
            }
        } else {
            return RESPEncoder.error("-ERR wrong number of arguments for 'SET'");
        }
    }

    private String handleGet(RedisCommand command) {
        if (command.args().size() != 1) {
            return RESPEncoder.error("ERR wrong number of arguments for 'GET'");
        }
        String val = DataStore.getInstance().get(command.args().get(0));
        return val == null ? RESPEncoder.nullString() : RESPEncoder.bulk(val);
    }

    private String handleDel(RedisCommand command, SnapshotType mode) {
        if (command.args().isEmpty()) {
            return RESPEncoder.error("ERR wrong number of arguments for 'DEL'");
        }
        if (SnapshotType.AOF.equals(mode)) {
            AOFLogger.logCommand(command.toCommandString());
        }
        int count = 0;
        for (int i = 0; i < command.args().size(); i++) {
            if (DataStore.getInstance().del(command.args().get(i))) {
                count++;
            }
        }
        return RESPEncoder.integer(count);
    }

    private String handleExpire(RedisCommand command) {
        if (command.args().size() != 2) {
            return RESPEncoder.error("ERR wrong number of arguments for 'EXPIRE'");
        }
        String key = command.args().get(0);
        int ttl = Integer.parseInt(command.args().get(1));
        boolean ok = DataStore.getInstance().expire(key, ttl);
        return RESPEncoder.bool(ok);
    }

    private String handleTTL(RedisCommand command) {
        if (command.args().size() != 1) {
            return RESPEncoder.error("ERR wrong number of arguments for 'TTL'");
        }
        String key = command.args().getFirst();
        long ttl = DataStore.getInstance().ttl(key);
        return ttl >= 0 ? RESPEncoder.integer(ttl) : RESPEncoder.nullString();
    }

    private static String handleInfo() {
        Map<String, String> info = DataStore.getInstance().getInfo();
        return RESPEncoder.convertInfoMapToResp("SERVER", info);
    }
}
