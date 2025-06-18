package com.hoclamdev.snapshot;

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

    public static void replay(DataStore store) {
        File file = new File(FILE);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> parts = List.of(line.split(" "));
                if (parts.get(0).equalsIgnoreCase("SET") && parts.size() == 3) {
                    store.set(parts.get(1), parts.get(2));
                } else if (parts.get(0).equalsIgnoreCase("DEL") && parts.size() == 2) {
                    store.del(parts.get(1));
                }
            }
        } catch (IOException e) {
            log.error("AOF replay error: ", e);
        }
    }
}
