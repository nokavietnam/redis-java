package com.hoclamdev.protocal;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RESPParser {
    private final BufferedReader reader;

    public RESPParser(BufferedReader reader) {
        this.reader = reader;
    }

    public List<String> parseCommand() throws IOException {
        String line = reader.readLine();
        if (line == null || !line.startsWith("*")) {
            throw  new IOException("Invalid RESP");
        }
        int numArgs = Integer.parseInt(line.substring(1));
        List<String> args = new ArrayList<>();
        for (int i = 0; i < numArgs; ++i) {
            reader.readLine();
            args.add(reader.readLine());
        }
        return args;
    }
}
