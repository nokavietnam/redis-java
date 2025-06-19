package com.hoclamdev.job;

import com.hoclamdev.config.ConfigLoader;
import com.hoclamdev.store.DataStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExpiryCleaner {
    private static final Logger log = LogManager.getLogger(ExpiryCleaner.class);
    private final ScheduledExecutorService scheduler;

    private static class Holder {
        private static final ExpiryCleaner INSTANCE = new ExpiryCleaner();
    }

    private ExpiryCleaner() {
        scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    }

    public static ExpiryCleaner getInstance() {
        return Holder.INSTANCE;
    }

    public void start() {
        int interval = ConfigLoader.getInt("ttl.clean.interval", 1000); // milliseconds

        Runnable task = () -> {
            long now = System.currentTimeMillis();
            for (String key : new HashSet<>(DataStore.getInstance().getTTLMap().keySet())) {
                Long expireAt = DataStore.getInstance().getTTLMap().get(key);
                if (expireAt != null && expireAt < now) {
                    DataStore.getInstance().del(key);
                }
            }
        };

        scheduler.scheduleAtFixedRate(task, 0, interval, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.close();
        }
    }
}
