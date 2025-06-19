package com.hoclamdev.encoder;

import java.util.List;

public class RESPEncoder {

    // Simple String: +OK\r\n
    public static String simple(String msg) {
        return "+" + msg + "\r\n";
    }

    // Error: -ERR message\r\n
    public static String error(String msg) {
        return "-" + msg + "\r\n";
    }

    // Integer: :123\r\n
    public static String integer(long value) {
        return ":" + value + "\r\n";
    }

    // Bulk String: $3\r\nfoo\r\n  hoặc  $-1\r\n
    public static String bulk(String value) {
        if (value == null) {
            return "$-1\r\n"; // null RESP
        }
        return "$" + value.length() + "\r\n" + value + "\r\n";
    }

    // Array of bulk strings: *2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n
    public static String array(List<String> values) {
        if (values == null) {
            return "*-1\r\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("*").append(values.size()).append("\r\n");
        for (String val : values) {
            sb.append(bulk(val));
        }
        return sb.toString();
    }

    // Null array
    public static String nullArray() {
        return "*-1\r\n";
    }
}
