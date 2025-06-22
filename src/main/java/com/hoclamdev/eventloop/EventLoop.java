package com.hoclamdev.eventloop;

import com.hoclamdev.encoder.RESPEncoder;
import com.hoclamdev.handler.CommandRouter;
import com.hoclamdev.job.ExpiryCleaner;
import com.hoclamdev.job.SnapshotJob;
import com.hoclamdev.protocol.RESP3Parser;
import com.hoclamdev.protocol.data.RedisCommand;
import com.hoclamdev.snapshot.SnapShot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

public class EventLoop implements Closeable {
    private static final Logger log = LogManager.getLogger(EventLoop.class);

    public void start(int port) throws IOException {
        try (Selector selector = Selector.open(); ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(new InetSocketAddress(port));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            log.info("Event loop started on port: {}", port);

            // restore date from snapshot
            SnapShot.restore();

            // run background job
            SnapshotJob.getInstance().start();
            ExpiryCleaner.getInstance().start();

            handle(selector);
        }
    }

    private void handle(Selector selector) throws IOException {
        while (!Thread.currentThread().isInterrupted()) {
            selector.select();
            final Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iter = keys.iterator();
            while (iter.hasNext()) {
                //for (Iterator<SelectionKey> iter = keys.iterator(); iter.hasNext();) {
                SelectionKey key = iter.next();
                iter.remove();
                try {
                    if (key.isValid()) {
                        if (key.isAcceptable()) {
                            log.info("Acceptable");
                            handleAccept(selector, key);
                        } else if (key.isReadable()) {
                            log.info("Reading");
                            handleRead(key);
                        } else if (key.isWritable()) {
                            log.info("Writing");
                            handleWrite(key);
                        } else if (key.isConnectable()) {
                            log.info("Connectable");
                            handleConnect();
                        }
                    }
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    private void handleConnect() {

    }

    private void handleAccept(Selector selector, SelectionKey key) throws IOException {
        log.info("Accept Thread: {}", Thread.currentThread().getName());
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverSocketChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(4096));
        log.info("New client connected: {}", clientChannel.getRemoteAddress());
        log.info("accepted {}", serverSocketChannel);
    }

    private void handleRead(SelectionKey key) {
        log.info("Read Thread: {}", Thread.currentThread().getName());
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        try {
            int bytesRead  = channel.read(buffer);
            if (bytesRead  == -1) {
                channel.close();
                key.cancel();
                log.warn("Client disconnected {}", channel.getRemoteAddress());
                return;
            }
            if (bytesRead == 0) return;

            buffer.flip();
            byte[] rawData = new byte[buffer.remaining()];
            buffer.get(rawData);
            buffer.clear();

            // Parse đầu vào RESP
            InputStream input = new ByteArrayInputStream(rawData);
            RESP3Parser parser = new RESP3Parser(input);

            while (!Thread.currentThread().isInterrupted()) {
                RedisCommand cmd;
                try {
                    cmd = parser.parseCommand(); // một lệnh RESP
                    if (cmd == null)
                        break;

                } catch (EOFException e) {
                    // dữ liệu chưa đầy đủ, đợi thêm trong lần sau
                    break;
                } catch (Exception ex) {
                    // trả lỗi RESP
                    String errResp = RESPEncoder.error("ERR " + ex.getMessage());
                    channel.write(ByteBuffer.wrap(errResp.getBytes(StandardCharsets.UTF_8)));
                    return;
                }
                // Xử lý command và encode kết quả RESP
                String response = CommandRouter.getInstance().processRESP(cmd);
                ByteBuffer outBuffer = ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8));
                channel.write(outBuffer);
            }
        } catch (SocketException se) {
            try {
                log.error("Socket reset: {}", se.getMessage());
                channel.close();
            } catch (IOException ignored) {}
            key.cancel();

        } catch (IOException e) {
            try {
                log.error("Read error: {}", e.getMessage());
                channel.close();
            } catch (IOException ignored) {}
            key.cancel();
        }
    }

    private static void handleWrite(final SelectionKey key) throws IOException {
        try (SocketChannel socket = (SocketChannel) key.channel()) {
            ByteBuffer byteBuffer = (ByteBuffer) key.attachment();
            socket.write(byteBuffer); // Wont always write everything
            while (!byteBuffer.hasRemaining()) {
                byteBuffer.compact();
                key.interestOps(SelectionKey.OP_READ);
            }
        } catch (SocketException se) {
            log.error("Socket reset: {}", se.getMessage());
        }
    }

    @Override
    public void close() {
        SnapshotJob.getInstance().stop();
        ExpiryCleaner.getInstance().stop();
    }
}
