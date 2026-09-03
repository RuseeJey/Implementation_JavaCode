package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private static final String URL = System.getenv().getOrDefault("DB_URL",
            "jdbc:mysql://localhost:3306/ipos_local?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    private static final String USER = System.getenv("DB_USER");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}