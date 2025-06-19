package com.hoclamdev.protocol;

import com.hoclamdev.protocol.data.RedisCommand;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RESP3Parser {
    private final BufferedReader reader;

    public RESP3Parser(InputStream inputStream) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
    }

    public synchronized RedisCommand parseCommand() throws IOException {
//        int prefix = reader.read();
//        if (prefix != '*') {
//            throw new IOException("Expected RESP array (*) for command, but got: " + (char) prefix);
//        }
//
//        String line = reader.readLine();
//        if (line == null || line.isEmpty()) {
//            throw new IOException("Missing argument count after '*'");
//        }

        String line = reader.readLine();

        if (line == null)
            throw new EOFException("No input from client");

        if (!line.startsWith("*")) {
            throw new IOException("Expected array header (*), got: " + line);
        }

        int argCount;
        try {
            argCount = Integer.parseInt(line.substring(1));
        } catch (NumberFormatException e) {
            throw new IOException("Invalid array count: " + line);
        }

        if (argCount < 1) {
            throw new IOException("Command must have at least one element");
        }

        // Parse command name
        Object cmdObj = parse();
        if (!(cmdObj instanceof String)) {
            throw new IOException("Expected command name as string");
        }
        String command = ((String) cmdObj).toUpperCase();

        List<String> args = new ArrayList<>();
        for (int i = 1; i < argCount; i++) {
            Object argObj = parse();
            if (!(argObj instanceof String)) {
                throw new IOException("Expected argument as string at index " + i);
            }
            args.add((String) argObj);
        }

        return new RedisCommand(command, args);
    }

    public synchronized Object parse() throws IOException {
        int prefix = reader.read();
        if (prefix == -1) return null;

        return switch (prefix) {
            case '$' -> parseBulk();
            case '+' -> parseSimpleString();
            case '-' -> parseError();
            case ':' -> parseLong();
            case '*' -> parseArray();
            case '~' -> parseSet();
            case '%' -> parseMap();
            case '#' -> parseBool();
            case '=' -> parseVerbatimString();
            case '(' -> parseBigNumber();
            case '|' -> parseAttributes(); // attributes map
            case ',' -> parseDouble();
            case '_' -> parseNull();
            default -> throw new IOException("Unknown RESP3 type: " + (char) prefix);
        };
    }

    public String parseSimpleString() throws IOException {
        return reader.readLine();
    }

    public String parseError() throws IOException {
        return "ERROR: " + reader.readLine();
    }

    public Long parseLong() throws IOException {
        return Long.parseLong(reader.readLine());
    }

    public Double parseDouble() throws IOException {
        return Double.parseDouble(reader.readLine());
    }

    public Boolean parseBool() throws IOException {
        String val = reader.readLine();
        return "t".equals(val);
    }

    public Object parseNull() throws IOException {
        skipCRLF(); // Consume line
        return null;
    }

    public String parseBulk() throws IOException {
        int len = Integer.parseInt(reader.readLine());
        if (len == -1) return null;
        char[] buf = new char[len];
        int read = reader.read(buf, 0, len);
        skipCRLF(); // skip \r\n
        return new String(buf, 0, read);
    }

    public List<Object> parseArray() throws IOException {
        int count = Integer.parseInt(reader.readLine());
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(parse());
        }
        return result;
    }

    public Set<Object> parseSet() throws IOException {
        int count = Integer.parseInt(reader.readLine());
        Set<Object> result = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            result.add(parse());
        }
        return result;
    }

    public Map<Object, Object> parseMap() throws IOException {
        int count = Integer.parseInt(reader.readLine());
        Map<Object, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            Object key = parse();
            Object val = parse();
            map.put(key, val);
        }
        return map;
    }

    private BigInteger parseBigNumber() throws IOException {
        String line = reader.readLine();
        String content = line.substring(1);
        return new BigInteger(content);
    }

    private String parseVerbatimString() throws IOException {
        String line = reader.readLine();
        String content = line.substring(1);
        // format =txt:Hello world
        int sep = content.indexOf(':');
        if (sep == -1) return content;
        String type = content.substring(0, sep);
        String value = content.substring(sep + 1);
        return "[" + type + "] " + value;
    }

    private Map<String, Object> parseAttributes() throws IOException {
        String line = reader.readLine();
        int count = Integer.parseInt(line);
        Map<String, Object> attrs = new LinkedHashMap<>();

        for (int i = 0; i < count; i++) {
            Object key = parse();
            Object value = parse();
            attrs.put(String.valueOf(key), value);
        }
        return attrs;
    }

    private void skipCRLF() throws IOException {
        reader.readLine(); // usually \r\n
    }
}
