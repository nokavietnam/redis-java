package com.hoclamdev.protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.EOFException;
import java.util.ArrayList;
import java.util.List;

public class RESPParser {
    private final BufferedReader reader;

    public RESPParser(InputStream inputStream) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
    }

    // Read 1 RESP command type array: *<count>\r\n$<len>\r\n<val>\r\n...
    public List<String> parseCommand() throws IOException {
        String line = reader.readLine();

        if (line == null) throw new EOFException("No input from client");

        if (!line.startsWith("*")) {
            throw new IOException("Expected array header (*), got: " + line);
        }

        int argCount;
        try {
            argCount = Integer.parseInt(line.substring(1));
        } catch (NumberFormatException e) {
            throw new IOException("Invalid argument count in RESP array", e);
        }

        List<String> args = new ArrayList<>();

        for (int i = 0; i < argCount; i++) {
            String lenLine = reader.readLine(); // $<length>
            if (lenLine == null || !lenLine.startsWith("$")) {
                throw new IOException("Expected bulk string header ($), got: " + lenLine);
            }

            int len;
            try {
                len = Integer.parseInt(lenLine.substring(1));
            } catch (NumberFormatException e) {
                throw new IOException("Invalid bulk string length", e);
            }

            char[] data = new char[len];
            int read = reader.read(data, 0, len);
            if (read < len) {
                throw new EOFException("Bulk string data incomplete");
            }

            reader.readLine(); // skip trailing \r\n

            args.add(new String(data));
        }

        return args;
    }
}

