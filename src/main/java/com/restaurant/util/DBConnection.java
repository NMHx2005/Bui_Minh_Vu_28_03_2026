package com.restaurant.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Lấy {@link Connection} từ {@code db.properties} trên classpath,
 * ghi đè bằng biến môi trường {@code JDBC_URL}, {@code JDBC_USER}, {@code JDBC_PASSWORD} nếu có.
 */
public final class DBConnection {

    private static final String PROP_URL = "jdbc.url";
    private static final String PROP_USER = "jdbc.username";
    private static final String PROP_PASSWORD = "jdbc.password";

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        Properties props = loadProperties();
        String url = firstNonBlank(System.getenv("JDBC_URL"), props.getProperty(PROP_URL));
        String user = firstNonBlank(System.getenv("JDBC_USER"), props.getProperty(PROP_USER));
        String password = firstNonBlank(System.getenv("JDBC_PASSWORD"), props.getProperty(PROP_PASSWORD));
        if (url == null || url.isBlank()) {
            throw new SQLException("Thiếu jdbc.url (db.properties hoặc JDBC_URL).");
        }
        return DriverManager.getConnection(url, user != null ? user : "", password != null ? password : "");
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được db.properties", e);
        }
        return props;
    }

    private static String firstNonBlank(String env, String file) {
        if (env != null && !env.isBlank()) {
            return env;
        }
        if (file != null && !file.isBlank()) {
            return file;
        }
        return null;
    }
}
