package com.hoclamdev.encoder;

import java.lang.management.ManagementFactory;

import com.hoclamdev.common.StringConstant;
import com.sun.management.OperatingSystemMXBean;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RESPEncoder {

    private RESPEncoder() {}

    // RESP3 Simple String: +OK\r\n
    public static String simple(String message) {
        return "+" + message + StringConstant.LINE_SEPARATOR;
    }

    // RESP3 Error: -ERR something\r\n
    public static String error(String message) {
        return "-" + message + StringConstant.LINE_SEPARATOR;
    }

    // RESP3 Integer: :123\r\n
    public static String integer(long value) {
        return ":" + value + StringConstant.LINE_SEPARATOR;
    }

    // RESP3 Double: ,3.1415\r\n
    public static String doubleValue(double value) {
        return "," + value + StringConstant.LINE_SEPARATOR;
    }

    // RESP3 Blob String: $3\r\nfoo\r\n
    public static String bulk(String value) {
        if (value == null) {
            return nullString();
        }
        return "$" + value.length() + StringConstant.LINE_SEPARATOR + value + StringConstant.LINE_SEPARATOR;
    }

    // RESP3 Verbatim String: =txt:hello\r\n
    public static String verbatim(String type, String content) {
        return "=" + type + ":" + content + StringConstant.LINE_SEPARATOR;
    }

    // RESP3 Null: _\r\n
    public static String nullString() {
        return "_\r\n";
    }

    // RESP3 Boolean: #t\r\n or #f\r\n
    public static String bool(boolean b) {
        return b ? "#t\r\n" : "#f\r\n";
    }

    // RESP3 Array: *2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n
    public static String array(List<String> values) {
        if (values == null) return "*-1\r\n";
        StringBuilder sb = new StringBuilder("*").append(values.size()).append(StringConstant.LINE_SEPARATOR);
        for (String val : values) {
            sb.append(bulk(val));
        }
        return sb.toString();
    }

    // RESP3 Map: %2\r\n+key\r\n+val\r\n...
    public static String map(Map<String, String> map) {
        if (map == null) return "*-1\r\n"; // or return "_\r\n"
        StringBuilder sb = new StringBuilder("%").append(map.size()).append(StringConstant.LINE_SEPARATOR);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(simple(entry.getKey()));
            sb.append(simple(entry.getValue()));
        }
        return sb.toString();
    }

    public static String arrayOfArrays(List<List<Object>> arrays) {
        StringBuilder sb = new StringBuilder("*" + arrays.size() + StringConstant.LINE_SEPARATOR);
        for (List<Object> sublist : arrays) {
            sb.append("*").append(sublist.size()).append(StringConstant.LINE_SEPARATOR);
            for (Object item : sublist) {
                sb.append(encodeObject(item));
            }
        }
        return sb.toString();
    }

    public static String encodeObject(Object obj) {
        if (obj == null) return "_\r\n";
        if (obj instanceof String str) return bulk(str);
        if (obj instanceof Long l) return integer(l);
        if (obj instanceof Integer i) return integer(i);
        if (obj instanceof Boolean b) return bool(b);
        if (obj instanceof List<?> list) return arrayRaw(list);
        throw new IllegalArgumentException("Unsupported RESP3 object: " + obj);
    }

    public static String arrayRaw(List<?> list) {
        StringBuilder sb = new StringBuilder("*" + list.size() + StringConstant.LINE_SEPARATOR);
        for (Object item : list) {
            sb.append(encodeObject(item));
        }
        return sb.toString();
    }

    public static String convertInfoMapToResp(String sectionName, Map<String, String> infoMap) {
        // 1. Build Redis-style INFO text
        StringBuilder sb = new StringBuilder();
        if (sectionName != null && !sectionName.isBlank()) {
            sb.append("# ").append(sectionName).append(StringConstant.LINE_SEPARATOR);
        }
        for (Map.Entry<String, String> entry : infoMap.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue()).append(StringConstant.LINE_SEPARATOR);
        }

        // 2. Wrap in RESP Bulk String
        return bulk(sb.toString());
    }

    public static String mapToRESP3(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("%").append(map.size()).append(StringConstant.LINE_SEPARATOR);

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append("+").append(entry.getKey()).append(StringConstant.LINE_SEPARATOR); // key is always a simple string

            Object value = entry.getValue();

            if (value instanceof String) {
                sb.append("+").append(value).append(StringConstant.LINE_SEPARATOR);
            } else if (value instanceof Integer || value instanceof Long) {
                sb.append(":").append(value).append(StringConstant.LINE_SEPARATOR);
            } else if (value instanceof List<?> list) {
                sb.append("*").append(list.size()).append(StringConstant.LINE_SEPARATOR);
                for (Object item : list) {
                    sb.append("+").append(item.toString()).append(StringConstant.LINE_SEPARATOR); // simplify: treat all list items as strings
                }
            } else if (value == null) {
                sb.append("_\r\n");
            } else {
                // fallback to string
                sb.append("+").append(value.toString()).append(StringConstant.LINE_SEPARATOR);
            }
        }

        return sb.toString();
    }
}

