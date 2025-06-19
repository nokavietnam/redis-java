package com.hoclamdev.server;

import com.hoclamdev.config.ConfigLoader;
import com.hoclamdev.job.ExpiryCleaner;
import com.hoclamdev.eventloop.EventLoop;
import com.hoclamdev.job.SnapshotJob;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class RedisServer {
    private static final Logger log = LogManager.getLogger(RedisServer.class);

    public static void main(String[] args) {
        int port = ConfigLoader.getInt("server.port", 6379);
        try (EventLoop eventLoop = new EventLoop()) {
            eventLoop.start(port);
        } catch (IOException ex) {
            log.error("Failed start server: ", ex);
        }
    }
}
