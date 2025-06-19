package com.hoclamdev.snapshot;

import com.hoclamdev.config.ConfigLoader;
import com.hoclamdev.store.DataStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SnapShot {
    private static final Logger log = LogManager.getLogger(SnapShot.class);
    public static void restore() {
        String mode = ConfigLoader.get("persistence.mode", "RDB").toUpperCase();
        if (mode.equals("RDB")) {
            RDBManager.loadSnapshot();
        } else if (mode.equals("AOF")) {
            AOFLogger.replay(DataStore.getInstance());
        } else {
            log.error("persistence.mode không hợp lệ: {}", mode);
        }
    }
}
