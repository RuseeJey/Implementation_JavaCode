package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserDB {

    public static class UserRow {
        private final String userId;
        private final String username;
        private final String role;

        public UserRow(String userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }

        public String getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getRole() {
            return role;
        }
    }

    public UserDB() {
        ensureTable();
    }

    private void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS SystemUser (" +
                "userID VARCHAR(64) PRIMARY KEY, " +
                "userName VARCHAR(100) NOT NULL UNIQUE, " +
                "password VARCHAR(255) NOT NULL, " +
                "role VARCHAR(50) NOT NULL)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<UserRow> getAllUsers() {
        List<UserRow> users = new ArrayList<>();
        String sql = "SELECT userID, userName, role FROM SystemUser ORDER BY userName";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(new UserRow(
                        rs.getString("userID"),
                        rs.getString("userName"),
                        rs.getString("role")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM SystemUser WHERE userName = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean usernameExistsForOtherUser(String userId, String username) {
        String sql = "SELECT 1 FROM SystemUser WHERE userName = ? AND userID <> ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean createUser(String username, String password, String role) {
        String sql = "INSERT INTO SystemUser (userID, userName, password, role) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, username);
            stmt.setString(3, password);
            stmt.setString(4, role);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteUser(String username) {
        String sql = "DELETE FROM SystemUser WHERE userName = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateUser(String userId, String username, String role) {
        String sql = "UPDATE SystemUser SET userName = ?, role = ? WHERE userID = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, role);
            stmt.setString(3, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean resetPassword(String username, String newPassword) {
        String sql = "UPDATE SystemUser SET password = ? WHERE userName = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newPassword);
            stmt.setString(2, username);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}

