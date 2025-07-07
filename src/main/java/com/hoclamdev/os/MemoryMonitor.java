package com.hoclamdev.os;

public class MemoryMonitor {
    public static long getUsedMemory() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    public static long getTotalMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    public static long getMaxJvmMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    public static boolean isMemoryExceeded(long thresholdBytes) {
        return getUsedMemory() >= thresholdBytes;
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int unit = 1024;
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "KMGTPE".charAt(exp - 1) + "i";
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
