package com.hoclamdev.snapshot;

import com.hoclamdev.snapshot.data.SnapshotData;
import com.hoclamdev.store.DataStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class RDBManager {
    private RDBManager() {}

    private static final Logger log = LogManager.getLogger(RDBManager.class);

    private static final String FILE = "dump.rdb";

    public static void saveSnapshot() throws IOException {
        SnapshotData snapshot = new SnapshotData(
                DataStore.getSnapshot(),
                DataStore.getTTLMapSnapshot()
        );
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE))) {
            out.writeObject(snapshot);
        }
    }

    @SuppressWarnings("unchecked")
    public static void loadSnapshot() {
        File file = new File(FILE);
        if (!file.exists()) return;

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            SnapshotData snapshot = (SnapshotData) in.readObject();
            DataStore.loadSnapshot(snapshot.getStore(), snapshot.getTtlMap());
        } catch (IOException | ClassNotFoundException e) {
            log.error("Failed to load RDB: ", e);
        }
    }
}
