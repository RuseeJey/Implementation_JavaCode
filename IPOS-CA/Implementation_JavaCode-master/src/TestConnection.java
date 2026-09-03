import database.DatabaseManager;
import java.sql.Connection;

public class TestConnection {
    public static void main(String[] args) {
        try (Connection conn = DatabaseManager.getConnection()) {
            System.out.println("Connected successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}