package com.hoclamdev.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static final Logger log = LogManager.getLogger(ConfigLoader.class);
    private static final Properties props = new Properties();

    static {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream("app.properties")) {
            props.load(input);
        } catch (IOException e) {
            log.error("Không load được file cấu hình: ", e);
        }
    }

    public static String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
