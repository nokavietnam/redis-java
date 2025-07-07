package com.hoclamdev.eventloop;

import com.hoclamdev.job.ExpiryCleaner;
import com.hoclamdev.job.SnapshotJob;
import com.hoclamdev.snapshot.SnapShot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class MultiCoreEventLoop implements Closeable {
    private static final Logger log = LogManager.getLogger(MultiCoreEventLoop.class);

    private final int numWorkers = Runtime.getRuntime().availableProcessors();
    private final Worker[] workers = new Worker[numWorkers];
    private int nextWorker = 0;

    public void start(int port) throws IOException {
        for (int i = 0; i < numWorkers; i++) {
            workers[i] = new Worker("worker-" + i);
            workers[i].start();
        }

        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(new InetSocketAddress(port));
            Selector acceptorSelector = Selector.open();
            serverChannel.register(acceptorSelector, SelectionKey.OP_ACCEPT);

            log.info("Event loop (multicore) started on port: {}", port);

            SnapShot.restore();
            SnapshotJob.getInstance().start();
            ExpiryCleaner.getInstance().start();

            while (!Thread.currentThread().isInterrupted()) {
                acceptorSelector.select();
                Set<SelectionKey> keys = acceptorSelector.selectedKeys();
                Iterator<SelectionKey> iter = keys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (key.isAcceptable()) {
                        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                        SocketChannel clientChannel = ssc.accept();
                        if (clientChannel != null) {
                            clientChannel.configureBlocking(false);
                            log.info("New connection: {}", clientChannel.getRemoteAddress());

                            Worker worker = getNextWorker();
                            worker.registerChannel(clientChannel);
                        }
                    }
                }
            }
        }
    }

    private synchronized Worker getNextWorker() {
        Worker worker = workers[nextWorker];
        nextWorker = (nextWorker + 1) % numWorkers;
        return worker;
    }

    @Override
    public void close() {
        for (Worker worker : workers) {
            worker.stopWorker();
        }
        SnapshotJob.getInstance().stop();
        ExpiryCleaner.getInstance().stop();
    }
}

