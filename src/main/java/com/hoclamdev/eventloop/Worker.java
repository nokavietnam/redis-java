package com.hoclamdev.eventloop;

import com.hoclamdev.encoder.RESPEncoder;
import com.hoclamdev.handler.CommandRouter;
import com.hoclamdev.protocol.RESP3Parser;
import com.hoclamdev.protocol.data.RedisCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

class Worker extends Thread {
    private static final Logger log = LogManager.getLogger(Worker.class);
    private final Selector selector;
    private final Queue<SocketChannel> newConnections = new ConcurrentLinkedQueue<>();
//    private final ByteBuffer buffer = ByteBuffer.allocate(4096);
    private volatile boolean running = true;

    public Worker(String name) throws IOException {
        super(name);
        this.selector = Selector.open();
    }

    public void registerChannel(SocketChannel channel) {
        newConnections.add(channel);
        selector.wakeup();
    }

    public void stopWorker() {
        running = false;
        selector.wakeup();
    }

    @Override
    public void run() {
        try {
            while (running) {
                selector.select();

                // Register new channels
                SocketChannel newChannel;
                while ((newChannel = newConnections.poll()) != null) {
                    newChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(4096));
                }

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iter = keys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Worker error: {}", e.getMessage());
        } finally {
            try {
                selector.close();
            } catch (IOException ignored) {}
        }
    }

    private void handleRead(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        try {
            int bytesRead = channel.read(buffer);
            if (bytesRead == -1) {
                channel.close();
                key.cancel();
                return;
            }

            if (bytesRead == 0) return;

            buffer.flip();
            byte[] rawData = new byte[buffer.remaining()];
            buffer.get(rawData);
            buffer.clear();

            RESP3Parser parser = new RESP3Parser(new ByteArrayInputStream(rawData));
            while (!Thread.currentThread().isInterrupted()) {
                RedisCommand cmd;
                try {
                    cmd = parser.parseCommand();
                    if (cmd == null) break;
                } catch (EOFException e) {
                    break;
                } catch (Exception ex) {
                    String errResp = RESPEncoder.error("ERR " + ex.getMessage());
                    channel.write(ByteBuffer.wrap(errResp.getBytes(StandardCharsets.UTF_8)));
                    return;
                }

                String response = CommandRouter.getInstance().processRESP(cmd);
                ByteBuffer outBuffer = ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8));
                channel.write(outBuffer);
            }

        } catch (IOException e) {
            try {
                log.error("Error reading from client: {}", e.getMessage());
                channel.close();
            } catch (IOException ignored) {}
            key.cancel();
        }
    }
}
