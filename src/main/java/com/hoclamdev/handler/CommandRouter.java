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
//            case "HGET":
//                return DataStore.hget(args[1], args[2]);
//            case "HDEL":
//                return DataStore.hdel(args[1], args[2]) ? 1 : 0;
//
//            // ZSET
//            case "ZADD":
//                DataStore.zadd(args[1], Double.parseDouble(args[2]), args[3]);
//                return "OK";
//            case "ZRANGE":
//                return DataStore.zrange(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]));
//            case "ZSCORE":
//                Double score = DataStore.zscore(args[1], args[2]);
//                return score != null ? score.toString() : null;
//            case "ZREM":
//                return DataStore.zrem(args[1], args[2]) ? 1 : 0;

//            case COMMAND -> handleCommand();
            default -> RESPEncoder.error("ERR unknown command");
        };
    }

    private String handleCommand() {
        Map<String, Object> commandMap = new LinkedHashMap<>();

        Map<String, Object> setCommand = new LinkedHashMap<>();
        setCommand.put("arity", 3);
        setCommand.put("flags", List.of("write", "denyoom"));
        setCommand.put("first-key", 1);
        setCommand.put("last-key", 1);
        setCommand.put("step", 1);
        setCommand.put("acl-categories", List.of("@write", "@keyspace"));
        setCommand.put("tips", List.of());

        Map<String, Object> keySpec = new LinkedHashMap<>();
        keySpec.put("flags", List.of("write"));

// begin-search
        Map<String, Object> beginSearch = new LinkedHashMap<>();
        beginSearch.put("type", "index");
        keySpec.put("begin-search", beginSearch);

// spec
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("index", 1);
        spec.put("flags", List.of());
        keySpec.put("spec", spec);

// find-key
        Map<String, Object> findKey = new LinkedHashMap<>();
        findKey.put("type", "range");

        Map<String, Object> rangeSpec = new LinkedHashMap<>();
        rangeSpec.put("start", 1);
        rangeSpec.put("end", 1);
        rangeSpec.put("flags", List.of());

        findKey.put("range-spec", rangeSpec);
        keySpec.put("find-key", findKey);

        setCommand.put("key-specs", List.of(keySpec));
        setCommand.put("subcommands", List.of());
        setCommand.put("engine", null);

        commandMap.put("set", setCommand);

        return RESPEncoder.mapToRESP3(commandMap);
    }

    private String handleHello(RedisCommand command) {
        if (command.args().size() >= 3 && command.args().get(1).equals("AUTH")) {
            String username = command.args().get(2);
            String password = command.args().get(3);
            log.info("Authenticating user '{}' with password '{}'", username, password);
            // TODO: implement auth
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
        String msg = !command.args().isEmpty() ? command.args().getFirst() : "PONG";
        //return RESPEncoder.verbatim("txt", msg);
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
