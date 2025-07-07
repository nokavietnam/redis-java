package com.hoclamdev.job;

import com.hoclamdev.common.SnapshotType;
import com.hoclamdev.config.ConfigLoader;
import com.hoclamdev.snapshot.RDBManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SnapshotJob {
    private static final Logger log = LogManager.getLogger(SnapshotJob.class);
    private ScheduledExecutorService scheduler = null;

    private SnapshotJob() {}

    private static class Holder {
        private static final SnapshotJob INSTANCE = new SnapshotJob();
    }

    public static SnapshotJob getInstance() {
        return Holder.INSTANCE;
    }

    public void start() {
        SnapshotType mode = SnapshotType.fromString(ConfigLoader.get("persistence.mode", "RDB").toUpperCase());
        if (!SnapshotType.RDB.equals(mode)) {
            return;
        }

        int interval = ConfigLoader.getInt("rdb.snapshot.interval", 30);
        scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
        Runnable task = () -> {
            try {
                RDBManager.saveSnapshot();
                log.info("RDB snapshot saved.");
            } catch (Exception e) {
                log.error("RDB save error: ", e);
            }
        };
        scheduler.scheduleAtFixedRate(task, interval, interval, TimeUnit.SECONDS);
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.close();
        }
    }
}
