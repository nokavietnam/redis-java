package com.hoclamdev.snapshot;

import com.hoclamdev.common.CommandType;
import com.hoclamdev.store.DataStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class AOFLogger {

    private static final Logger log = LogManager.getLogger(AOFLogger.class);
    private static final String FILE = "appendonly.aof";

    public static synchronized void logCommand(List<String> cmd) {
        try (FileWriter fw = new FileWriter(FILE, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(String.join(" ", cmd));
            bw.newLine();
        } catch (IOException e) {
            log.error("AOF write error: ", e);
        }
    }

    public static synchronized void logCommand(String cmd) {
        try (FileWriter fw = new FileWriter(FILE, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(cmd);
            bw.newLine();
        } catch (IOException e) {
            log.error("AOF write error: ", e);
        }
    }

    public static void replay(DataStore store) {
        File file = new File(FILE);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> command = List.of(line.split(" "));
                switch (CommandType.fromString(command.getFirst())) {
                    case SET: {
                        replaySet(store, command);
                        break;
                    }
                    case DEL: {
                        replayDel(store, command);
                        break;
                    }
                    case RPUSH: {
                        replayRPush(store, command);
                        break;
                    }
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            log.error("AOF replay error: ", e);
        }
    }

    private static void replaySet(DataStore store, List<String> command) {
        if (command.size() == 3) {
            store.set(command.get(1), command.get(2));
        }
    }

    private static void replayDel(DataStore store, List<String> command) {
        if (command.size() == 2) {
            store.del(command.get(1));
        }
    }

    private static void replayRPush(DataStore store, List<String> command) {
        if (command.size() == 3) {
            store.rpush(command.get(1), command.get(2));
        }
    }
}
