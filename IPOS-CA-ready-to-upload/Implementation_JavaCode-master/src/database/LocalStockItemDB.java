package database;

import IPOS_CA_STOCK.LocalStockItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LocalStockItemDB {

    public LocalStockItemDB() {}

    private LocalStockItem mapRow(ResultSet rs) throws SQLException {
        LocalStockItem item = new LocalStockItem();
        item.setItemID(rs.getString("itemID"));
        item.setItemName(rs.getString("itemName"));
        item.setPackageType(rs.getString("packageType"));
        item.setUnitType(rs.getString("unitType"));
        item.setUnitsPerPack(rs.getInt("unitsPerPack"));
        item.setWholesaleCost(rs.getDouble("wholesaleCost"));
        item.setMarkupRate(rs.getDouble("markupRate"));
        item.setVatRate(rs.getDouble("vatRate"));
        item.setQuantity(rs.getInt("quantity"));
        item.setMinimumStockLevel(rs.getInt("minimumStockLevel"));
        return item;
    }

    public List<LocalStockItem> getAllItems() {
        List<LocalStockItem> items = new ArrayList<>();
        String sql = "SELECT * FROM LocalStockItem ORDER BY itemID";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) items.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return items;
    }

    public LocalStockItem getItemByID(String itemID) {
        String sql = "SELECT * FROM LocalStockItem WHERE itemID = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, itemID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public boolean addItem(LocalStockItem item) {
        String sql = """
            INSERT INTO LocalStockItem
              (itemID, itemName, packageType, unitType, unitsPerPack,
               wholesaleCost, markupRate, vatRate, quantity, minimumStockLevel)
            VALUES (?,?,?,?,?,?,?,?,?,?)
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getItemID());
            ps.setString(2, item.getItemName());
            ps.setString(3, item.getPackageType() != null ? item.getPackageType() : "Box");
            ps.setString(4, item.getUnitType() != null ? item.getUnitType() : "Caps");
            ps.setInt(5,    item.getUnitsPerPack());
            ps.setDouble(6, item.getWholesaleCost());
            ps.setDouble(7, item.getMarkupRate());
            ps.setDouble(8, item.getVatRate());
            ps.setInt(9,    item.getQuantity());
            ps.setInt(10,   item.getMinimumStockLevel());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean updateItem(LocalStockItem item) {
        String sql = """
            UPDATE LocalStockItem SET
              itemName=?, packageType=?, unitType=?, unitsPerPack=?,
              wholesaleCost=?, markupRate=?, vatRate=?,
              quantity=?, minimumStockLevel=?
            WHERE itemID=?
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getItemName());
            ps.setString(2, item.getPackageType());
            ps.setString(3, item.getUnitType());
            ps.setInt(4,    item.getUnitsPerPack());
            ps.setDouble(5, item.getWholesaleCost());
            ps.setDouble(6, item.getMarkupRate());
            ps.setDouble(7, item.getVatRate());
            ps.setInt(8,    item.getQuantity());
            ps.setInt(9,    item.getMinimumStockLevel());
            ps.setString(10, item.getItemID());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean updateQuantity(String itemID, int newQuantity) {
        String sql = "UPDATE LocalStockItem SET quantity=? WHERE itemID=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newQuantity);
            ps.setString(2, itemID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean increaseStock(String itemID, int qty) {
        String sql = "UPDATE LocalStockItem SET quantity = quantity + ? WHERE itemID=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setString(2, itemID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean decreaseStock(String itemID, int qty) {
        String sql = "UPDATE LocalStockItem SET quantity = quantity - ? WHERE itemID=? AND quantity >= ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setString(2, itemID);
            ps.setInt(3, qty);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean updateMinimumStockLevel(String itemID, int level) {
        String sql = "UPDATE LocalStockItem SET minimumStockLevel=? WHERE itemID=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, level);
            ps.setString(2, itemID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean deleteItem(String itemID) {
        String sql = "DELETE FROM LocalStockItem WHERE itemID=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, itemID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public List<LocalStockItem> searchItems(String keyword) {
        List<LocalStockItem> items = new ArrayList<>();
        String sql = "SELECT * FROM LocalStockItem WHERE itemID LIKE ? OR itemName LIKE ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String kw = "%" + keyword + "%";
            ps.setString(1, kw); ps.setString(2, kw);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) items.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return items;
    }

    public List<LocalStockItem> getBelowMinimumStock() {
        List<LocalStockItem> items = new ArrayList<>();
        String sql = "SELECT * FROM LocalStockItem WHERE quantity < minimumStockLevel ORDER BY itemID";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) items.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return items;
    }
}