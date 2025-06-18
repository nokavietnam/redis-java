package com.hoclamdev.cleaner;

import com.hoclamdev.config.ConfigLoader;
import com.hoclamdev.store.DataStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExpiryCleaner {

    private static final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());

    private static final Logger log = LogManager.getLogger(ExpiryCleaner.class);

//    public ExpiryCleaner() {
//        setDaemon(true);
//        this.intervalMs = ConfigLoader.getInt("ttl.clean.interval", 1000);
//    }

    public static void start() {
        int interval = ConfigLoader.getInt("ttl.clean.interval", 1000); // milliseconds

        Runnable task = () -> {
            long now = System.currentTimeMillis();
            for (String key : new HashSet<>(DataStore.getTTLMap().keySet())) {
                Long expireAt = DataStore.getTTLMap().get(key);
                if (expireAt != null && expireAt < now) {
                    DataStore.del(key);
                }
            }
        };

        scheduler.scheduleAtFixedRate(task, 0, interval, TimeUnit.MILLISECONDS);
    }
}
