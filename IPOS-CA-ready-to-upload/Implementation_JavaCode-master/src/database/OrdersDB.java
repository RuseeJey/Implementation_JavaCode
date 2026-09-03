package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrdersDB {

    public static class OrderSummary {
        private final String orderId, status;
        private final double totalCost;
        private final Timestamp createdAt;
        private final String stockName;
        private final int quantity;

        public OrderSummary(String orderId, String status, double totalCost,
                            Timestamp createdAt, String stockName, int quantity) {
            this.orderId = orderId; this.status = status;
            this.totalCost = totalCost; this.createdAt = createdAt;
            this.stockName = stockName; this.quantity = quantity;
        }
        public String    getOrderId()    { return orderId; }
        public String    getStatus()     { return status; }
        public double    getTotalCost()  { return totalCost; }
        public Timestamp getCreatedAt()  { return createdAt; }
        public String    getStockName()  { return stockName; }
        public int       getQuantity()   { return quantity; }
    }

    public static class CatalogueItem {
        private final String itemId, itemName;
        private final double unitPrice;
        public CatalogueItem(String itemId, String itemName, double unitPrice) {
            this.itemId = itemId; this.itemName = itemName; this.unitPrice = unitPrice;
        }
        public String getItemId()    { return itemId; }
        public String getItemName()  { return itemName; }
        public double getUnitPrice() { return unitPrice; }
    }

    public static class OrderItem {
        private final String itemId, itemName;
        private final double unitPrice;
        private final int quantity;
        public OrderItem(String itemId, String itemName, double unitPrice, int quantity) {
            this.itemId = itemId; this.itemName = itemName;
            this.unitPrice = unitPrice; this.quantity = quantity;
        }
        public String getItemId()    { return itemId; }
        public String getItemName()  { return itemName; }
        public double getUnitPrice() { return unitPrice; }
        public int    getQuantity()  { return quantity; }
    }

    public static class OrderItemDetail {
        private final String itemId, itemName;
        private final int quantity;
        public OrderItemDetail(String itemId, String itemName, int quantity) {
            this.itemId = itemId; this.itemName = itemName; this.quantity = quantity;
        }
        public String getItemId()   { return itemId; }
        public String getItemName() { return itemName; }
        public int    getQuantity() { return quantity; }
    }

    // ── PU Order row — matches ca_inventory_adjustments exactly ──────────────

    public static class PUOrderRow {
        private final String adjustmentId, orderId, merchantId, itemId;
        private final int qty;
        private final String deliveryAddress, status, createdAt, processedAt, error;

        public PUOrderRow(String adjustmentId, String orderId, String merchantId,
                          String itemId, int qty, String deliveryAddress,
                          String status, String createdAt, String processedAt, String error) {
            this.adjustmentId    = adjustmentId;
            this.orderId         = orderId;
            this.merchantId      = merchantId;
            this.itemId          = itemId;
            this.qty             = qty;
            this.deliveryAddress = deliveryAddress;
            this.status          = status;
            this.createdAt       = createdAt;
            this.processedAt     = processedAt;
            this.error           = error;
        }

        public String getAdjustmentId()    { return adjustmentId; }
        public String getOrderId()         { return orderId; }
        public String getMerchantId()      { return merchantId; }
        public String getItemId()          { return itemId; }
        public int    getQty()             { return qty; }
        public String getDeliveryAddress() { return deliveryAddress; }
        public String getStatus()          { return status; }
        public String getCreatedAt()       { return createdAt; }
        public String getProcessedAt()     { return processedAt; }
        public String getError()           { return error; }
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    public OrdersDB() {}

    public String getLastSubmitError() { return ""; }

    // ── SA Catalogue ──────────────────────────────────────────────────────────

    public List<CatalogueItem> getCatalogueItems() {
        List<CatalogueItem> items = new ArrayList<>();
        String sql = "SELECT itemID, itemName, unitPrice FROM SACatalogue ORDER BY itemName";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) items.add(new CatalogueItem(
                    rs.getString("itemID"),
                    rs.getString("itemName"),
                    rs.getDouble("unitPrice")));
        } catch (SQLException e) { e.printStackTrace(); }
        return items;
    }

    // ── Place order with SA ───────────────────────────────────────────────────

    public String submitOrder(List<OrderItem> items) {
        if (items == null || items.isEmpty()) return null;

        String orderId  = "ORD-CA-" + System.currentTimeMillis();
        String orderSQL = """
            INSERT INTO SupplierOrder
              (orderID, orderStatus, totalCost, dateOrdered)
            VALUES (?,?,?,CURDATE())
            """;
        String lineSQL  = """
            INSERT INTO SupplierOrderItem
              (orderID, itemID, itemName, quantity, unitCost, lineTotal)
            VALUES (?,?,?,?,?,?)
            """;

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            double total = 0.0;
            for (OrderItem item : items) total += item.getUnitPrice() * item.getQuantity();

            try (PreparedStatement ps = conn.prepareStatement(orderSQL)) {
                ps.setString(1, orderId);
                ps.setString(2, "accepted");
                ps.setDouble(3, total);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(lineSQL)) {
                for (OrderItem item : items) {
                    ps.setString(1, orderId);
                    ps.setString(2, item.getItemId());
                    ps.setString(3, item.getItemName());
                    ps.setInt(4,    item.getQuantity());
                    ps.setDouble(5, item.getUnitPrice());
                    ps.setDouble(6, item.getUnitPrice() * item.getQuantity());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
            return orderId;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            return null;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); }
            catch (SQLException ignored) {}
        }
    }

    // ── Order history & tracking ──────────────────────────────────────────────

    public List<OrderSummary> getOrderHistory() {
        List<OrderSummary> history = new ArrayList<>();
        String sql = """
            SELECT o.orderID, o.orderStatus, o.totalCost, o.dateOrdered,
                   COALESCE(GROUP_CONCAT(oi.itemName ORDER BY oi.itemName SEPARATOR ', '),'') AS stockName,
                   COALESCE(SUM(oi.quantity),0) AS quantity
            FROM SupplierOrder o
            LEFT JOIN SupplierOrderItem oi ON oi.orderID = o.orderID
            GROUP BY o.orderID, o.orderStatus, o.totalCost, o.dateOrdered
            ORDER BY o.dateOrdered DESC
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) history.add(new OrderSummary(
                    rs.getString("orderID"),
                    rs.getString("orderStatus"),
                    rs.getDouble("totalCost"),
                    rs.getTimestamp("dateOrdered"),
                    rs.getString("stockName"),
                    rs.getInt("quantity")));
        } catch (SQLException e) { e.printStackTrace(); }
        return history;
    }

    public OrderSummary getOrderSummary(String orderId) {
        String sql = "SELECT orderID, orderStatus, totalCost, dateOrdered FROM SupplierOrder WHERE orderID=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new OrderSummary(
                    rs.getString("orderID"), rs.getString("orderStatus"),
                    rs.getDouble("totalCost"), rs.getTimestamp("dateOrdered"), "", 0);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public boolean updateOrderStatus(String orderId, String status) {
        if (orderId == null || status == null) return false;
        List<String> valid = List.of("accepted","ready to dispatch","dispatched","delivered","cancelled");
        if (!valid.contains(status.toLowerCase())) return false;

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            String currentStatus;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT orderStatus FROM SupplierOrder WHERE orderID=? FOR UPDATE")) {
                ps.setString(1, orderId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) { conn.rollback(); return false; }
                currentStatus = rs.getString("orderStatus");
            }

            if ("delivered".equalsIgnoreCase(status)
                    && !"delivered".equalsIgnoreCase(currentStatus)) {
                applyDeliveredStockIncrease(conn, orderId);
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE SupplierOrder SET orderStatus=? WHERE orderID=?")) {
                ps.setString(1, status.toLowerCase());
                ps.setString(2, orderId);
                ps.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); }
            catch (SQLException ignored) {}
        }
    }

    private void applyDeliveredStockIncrease(Connection conn, String orderId) throws SQLException {
        String sql = "SELECT itemID, quantity FROM SupplierOrderItem WHERE orderID=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE LocalStockItem SET quantity = quantity + ? WHERE itemID=?")) {
                    upd.setInt(1, rs.getInt("quantity"));
                    upd.setString(2, rs.getString("itemID"));
                    upd.executeUpdate();
                }
            }
        }
    }

    public boolean cancelPendingOrderItems(String orderId) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT orderStatus FROM SupplierOrder WHERE orderID=? FOR UPDATE")) {
                ps.setString(1, orderId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next() || !"accepted".equalsIgnoreCase(rs.getString("orderStatus"))) {
                    conn.rollback(); return false;
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE SupplierOrder SET orderStatus='cancelled' WHERE orderID=?")) {
                ps.setString(1, orderId); ps.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); }
            catch (SQLException ignored) {}
        }
    }

    public List<OrderItemDetail> getOrderItemsByOrderId(String orderId) {
        List<OrderItemDetail> details = new ArrayList<>();
        String sql = "SELECT itemID, itemName, quantity FROM SupplierOrderItem WHERE orderID=? ORDER BY itemID";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) details.add(new OrderItemDetail(
                    rs.getString("itemID"),
                    rs.getString("itemName"),
                    rs.getInt("quantity")));
        } catch (SQLException e) { e.printStackTrace(); }
        return details;
    }

    // ── PU Portal orders — reads from ca_inventory_adjustments ───────────────

    public List<PUOrderRow> getAllPUOrders() {
        List<PUOrderRow> list = new ArrayList<>();
        String sql = """
            SELECT adjustment_id, order_id, merchant_id, item_id, qty,
                   COALESCE(delivery_address,'') AS delivery_address,
                   status, created_at, processed_at,
                   COALESCE(error,'') AS error
            FROM ca_inventory_adjustments
            ORDER BY created_at DESC, adjustment_id DESC
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new PUOrderRow(
                    rs.getString("adjustment_id"),
                    rs.getString("order_id"),
                    rs.getString("merchant_id"),
                    rs.getString("item_id"),
                    rs.getInt("qty"),
                    rs.getString("delivery_address"),
                    rs.getString("status"),
                    rs.getTimestamp("created_at") != null
                            ? rs.getTimestamp("created_at").toString() : "",
                    rs.getTimestamp("processed_at") != null
                            ? rs.getTimestamp("processed_at").toString() : "",
                    rs.getString("error")));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean updatePUOrderStatus(String adjustmentId, String newStatus) {
        if (adjustmentId == null || newStatus == null) return false;
        String upper = newStatus.trim().toUpperCase();
        if (!upper.equals("PENDING") && !upper.equals("APPLIED")
                && !upper.equals("FAILED") && !upper.equals("CANCELLED")) return false;

        String sql = """
            UPDATE ca_inventory_adjustments
            SET status = ?,
                processed_at = CASE WHEN ? = 'PENDING' THEN NULL ELSE NOW() END
            WHERE adjustment_id = ?
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, upper);
            ps.setString(2, upper);
            ps.setInt(3, Integer.parseInt(adjustmentId.trim()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
        catch (NumberFormatException e) { return false; }
    }
}