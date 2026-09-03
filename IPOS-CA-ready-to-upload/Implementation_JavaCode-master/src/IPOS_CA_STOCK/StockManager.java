package IPOS_CA_STOCK;

import IPOS_CA_ORD.OrderItem;
import database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class StockManager {

    /**
     * Deduct stock for each item in an order
     */
    public boolean deductStock(String orderID, List<OrderItem> items) {

        String updateSQL = "UPDATE LocalStockItem SET quantity = quantity - ? WHERE itemID = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSQL)) {

            conn.setAutoCommit(false); // transaction start

            for (OrderItem item : items) {

                stmt.setInt(1, item.getQuantity()); // quantity to deduct
                stmt.setString(2, item.getItemID());

                stmt.addBatch();
            }

            stmt.executeBatch();
            conn.commit(); // commit transaction

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get current stock quantity for an item
     */
    public int getQuantity(String itemID) {

        String sql = "SELECT quantity FROM LocalStockItem WHERE itemID = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, itemID);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("quantity");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0; // if not found
    }
}
