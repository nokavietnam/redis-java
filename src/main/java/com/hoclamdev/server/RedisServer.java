package com.hoclamdev.server;

import com.hoclamdev.cleaner.ExpiryCleaner;
import com.hoclamdev.config.ConfigLoader;
import com.hoclamdev.handler.CommandHandler;
import com.hoclamdev.handler.GetSetDelHandler;
import com.hoclamdev.protocol.RESPParser;
import com.hoclamdev.snapshot.AOFLogger;
import com.hoclamdev.snapshot.RDBManager;
import com.hoclamdev.store.DataStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RedisServer {
    private static final Logger log = LogManager.getLogger(RedisServer.class);

    public static void main(String[] args) {
        String mode = ConfigLoader.get("persistence.mode", "RDB").toUpperCase();
        ScheduledExecutorService scheduler = null;

        if (mode.equals("RDB")) {
            RDBManager.loadSnapshot();
            int interval = ConfigLoader.getInt("rdb.snapshot.interval", 30);

            scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    RDBManager.saveSnapshot();
                    log.info("RDB snapshot saved.");
                } catch (Exception e) {
                    log.error("RDB save error: ", e);
                }
            }, interval, interval, TimeUnit.SECONDS);

//            new Thread(() -> {
//                while (true) {
//                    try {
//                        Thread.sleep(interval * 1000L);
//                        RDBManager.saveSnapshot();
//                        log.info("RDB snapshot saved.");
//                    } catch (Exception e) {
//                        log.error("RDB error: ", e);
//                        Thread.currentThread().interrupt();
//                    }
//                }
//            }).start();
        } else if (mode.equals("AOF")) {
            AOFLogger.replay(DataStore.getInstance());
        } else {
            log.error("persistence.mode không hợp lệ: {}", mode);
        }

        try (ServerSocket serverSocket = new ServerSocket(6379);) {
            log.info("Redis Java started on port 6379");
            // Start the TTL background cleaner thread
            ExpiryCleaner.start();
            // Accept and handle clients
            while (!Thread.currentThread().isInterrupted()) {
                Socket client = serverSocket.accept();
                Thread.ofVirtual().start(() ->  handleClient(client));
            }
        } catch (Exception ex) {
            log.error("Server start error: ", ex);
        } finally {
            if (scheduler != null) {
                scheduler.close();
            }
        }
    }

    public static void handleClient(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()))) {
            RESPParser parser = new RESPParser(in);
            CommandHandler handler = new GetSetDelHandler();
            while (!Thread.currentThread().isInterrupted()) {
                List<String> command = parser.parseCommand();
                String response = handler.handle(command);
                out.write(response);
                out.flush();
            }
        } catch (IOException ex) {
            log.error("Client disconnected: ", ex);
        }
    }
}
