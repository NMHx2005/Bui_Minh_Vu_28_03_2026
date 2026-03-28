package com.restaurant;

import com.restaurant.util.DBConnection;
import com.restaurant.util.PasswordHasher;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Buổi 1: kiểm tra kết nối MySQL và xác minh mật khẩu seed (bcrypt).
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {

            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) AS c FROM users")) {
                if (rs.next()) {
                    System.out.println("Kết nối OK. Số user trong DB: " + rs.getInt("c"));
                }
            }

            String managerHash = null;
            try (ResultSet rs = st.executeQuery(
                    "SELECT password_hash FROM users WHERE username = 'manager'")) {
                if (rs.next()) {
                    managerHash = rs.getString("password_hash");
                }
            }
            if (managerHash != null) {
                boolean ok = PasswordHasher.verify("Manager@123", managerHash);
                System.out.println("Kiểm tra bcrypt (manager / Manager@123): " + (ok ? "đúng" : "sai"));
            }
        } catch (Exception e) {
            System.err.println("Lỗi: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
