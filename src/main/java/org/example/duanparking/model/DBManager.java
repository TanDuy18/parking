package org.example.duanparking.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/parking_entries?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "TanDui18@IT.com";

    public static boolean saveEntry(String plate, String arrival, String owner) {
        String sql = "INSERT INTO parking_entries (plate, arrival_time, owner) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, plate);
            pstmt.setString(2, arrival);
            pstmt.setString(3, owner);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
