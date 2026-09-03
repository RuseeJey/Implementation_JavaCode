package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SalesDB {

    private static final String FIXED_DISCOUNT_PREFIX = "FIXED|";
    private static final String FLEX_DISCOUNT_PREFIX = "FLEX|";

    public static class SaleItem {
        private final String itemId, itemName;
        private final double unitPrice;
        private final int quantity;
        public SaleItem(String itemId, String itemName, double unitPrice, int quantity) {
            this.itemId = itemId; this.itemName = itemName;
            this.unitPrice = unitPrice; this.quantity = quantity;
        }
        public String getItemId()    { return itemId; }
        public String getItemName()  { return itemName; }
        public double getUnitPrice() { return unitPrice; }
        public int getQuantity()     { return quantity; }
    }

    // ── Main sale submission (legacy — no card details) ───────────────────────

    public String submitSale(String customerType, String customerName, List<SaleItem> items) {
        return submitSale(customerType, customerName, items, new Date(System.currentTimeMillis()));
    }

    public String submitSale(String customerType, String customerName,
                             List<SaleItem> items, Date saleDate) {
        return submitSaleWithPayment(customerType, customerName, items, saleDate,
                "card", null, null, null, null);
    }

    // ── Full sale submission with payment details ─────────────────────────────

    public String submitSaleWithPayment(String customerType, String customerName,
                                        List<SaleItem> items, Date saleDate,
                                        String paymentMethod, String cardType,
                                        String cardFirst4, String cardLast4,
                                        String cardExpiry) {
        if (items == null || items.isEmpty()) return null;

        // Aggregate duplicate items
        Map<String, SaleItem> agg = new LinkedHashMap<>();
        for (SaleItem item : items) {
            if (item == null || item.getItemId() == null || item.getItemId().trim().isEmpty()) continue;
            SaleItem existing = agg.get(item.getItemId());
            agg.put(item.getItemId(), existing == null ? item :
                    new SaleItem(existing.getItemId(), existing.getItemName(),
                            existing.getUnitPrice(), existing.getQuantity() + item.getQuantity()));
        }
        if (agg.isEmpty()) return null;

        String normName   = customerName == null ? null : customerName.trim();
        boolean isAccount = "register".equalsIgnoreCase(customerType);
        String txnID      = "TXN-" + System.currentTimeMillis();

        double subTotal = 0.0;
        for (SaleItem item : agg.values()) subTotal += item.getUnitPrice() * item.getQuantity();

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            String customerID   = null;
            double discountRate = 0.0;
            double total        = roundCurrency(subTotal);

            if (isAccount && normName != null && !normName.isEmpty()) {
                String creditSQL = """
                    SELECT customerID, creditLimit, currentBalance, accountStatus, discountPlan
                    FROM CustomerAccount WHERE name = ? LIMIT 1
                    """;
                try (PreparedStatement ps = conn.prepareStatement(creditSQL)) {
                    ps.setString(1, normName);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) { conn.rollback(); return null; }
                    customerID = rs.getString("customerID");
                    if ("in default".equals(rs.getString("accountStatus"))) {
                        conn.rollback(); return null;
                    }
                    discountRate = getDiscountRateForCustomer(conn, normName, subTotal);
                    double limit   = rs.getDouble("creditLimit");
                    double balance = rs.getDouble("currentBalance");
                    total          = roundCurrency(subTotal * (1.0 - discountRate));
                    if (balance + total > limit) { conn.rollback(); return null; }
                }
            }

            double discountAmount = roundCurrency(subTotal - total);

            // Trim card digits to 4 chars max
            String cf4 = cardFirst4 != null && cardFirst4.length() > 4
                    ? cardFirst4.substring(0, 4) : cardFirst4;
            String cl4 = cardLast4 != null && cardLast4.length() > 4
                    ? cardLast4.substring(0, 4) : cardLast4;

            // Insert SaleTransaction with all required columns
            String txSQL = """
                INSERT INTO SaleTransaction
                  (transactionID, customerID, customerType, customerName, saleDate,
                   paymentMethod, cardType, cardFirst4, cardLast4, cardExpiry,
                   subTotal, discountAmount, vatAmount, totalAmount, source)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,'in-store')
                """;
            try (PreparedStatement ps = conn.prepareStatement(txSQL)) {
                ps.setString(1, txnID);
                ps.setString(2, customerID);
                ps.setString(3, isAccount ? "account" : "occasional");
                ps.setString(4, normName);
                ps.setDate(5, saleDate != null ? saleDate : new Date(System.currentTimeMillis()));
                ps.setString(6, paymentMethod);
                ps.setString(7, cardType);
                ps.setString(8, cf4);
                ps.setString(9, cl4);
                ps.setString(10, cardExpiry);
                ps.setDouble(11, subTotal);
                ps.setDouble(12, discountAmount);
                ps.setDouble(13, 0.0);
                ps.setDouble(14, total);
                ps.executeUpdate();
            }

            // Insert line items and deduct stock
            String lineSQL = """
                INSERT INTO LocalStockItem_SaleTransaction
                  (transactionID, itemID, quantitySold, unitRetailPrice, lineTotal)
                VALUES (?,?,?,?,?)
                """;
            String stockSQL = "UPDATE LocalStockItem SET quantity = quantity - ? WHERE itemID=? AND quantity >= ?";

            for (SaleItem item : agg.values()) {
                try (PreparedStatement ps = conn.prepareStatement(lineSQL)) {
                    ps.setString(1, txnID);
                    ps.setString(2, item.getItemId());
                    ps.setInt(3, item.getQuantity());
                    ps.setDouble(4, item.getUnitPrice());
                    ps.setDouble(5, item.getUnitPrice() * item.getQuantity());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(stockSQL)) {
                    ps.setInt(1, item.getQuantity());
                    ps.setString(2, item.getItemId());
                    ps.setInt(3, item.getQuantity());
                    if (ps.executeUpdate() == 0) { conn.rollback(); return null; }
                }
            }

            // Add to account balance using customerID
            if (isAccount && customerID != null) {
                String balSQL = "UPDATE CustomerAccount SET currentBalance = currentBalance + ? WHERE customerID=?";
                try (PreparedStatement ps = conn.prepareStatement(balSQL)) {
                    ps.setDouble(1, total);
                    ps.setString(2, customerID);
                    ps.executeUpdate();
                }
            }

            conn.commit();
            return txnID;

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            return null;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
        }
    }

    private double getDiscountRateForCustomer(Connection conn, String customerName, double subtotal) throws SQLException {
        if (customerName == null || customerName.trim().isEmpty()) {
            return 0.0;
        }

        String sql = "SELECT discountPlan FROM CustomerAccount WHERE name = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return 0.0;
                }

                String plan = rs.getString("discountPlan");
                return resolveFlexibleDiscountRate(plan, subtotal);
            }
        }
    }

    // Keep static method for SalesPanel live discount preview
    public static double resolveFlexibleDiscountRate(String plan, double subtotal) {
        if (plan == null || plan.trim().isEmpty()) {
            return 0.0;
        }

        String trimmed = plan.trim();
        String upper = trimmed.toUpperCase();

        if ("NONE".equals(upper)) {
            return 0.0;
        }

        if (upper.startsWith(FIXED_DISCOUNT_PREFIX)) {
            String rateText = trimmed.substring(FIXED_DISCOUNT_PREFIX.length()).trim();
            return parseRate(rateText);
        }

        if (!upper.startsWith(FLEX_DISCOUNT_PREFIX) && !"FLEX".equals(upper) && !"FLEXIBLE".equals(upper)) {
            return 0.0;
        }

        String[] tiers = trimmed.substring(FLEX_DISCOUNT_PREFIX.length()).split("\\|");
        if (tiers.length == 0) {
            return 0.0;
        }

        // Parse all tiers first
        double[] thresholds = new double[tiers.length];
        double[] rates = new double[tiers.length];

        for (String tier : tiers) {
            if (tier == null || tier.trim().isEmpty()) {
                continue;
            }

            String[] parts = tier.split(":", 2);
            if (parts.length != 2) {
                continue;
            }

            String thresholdText = parts[0].trim();
            String rateText = parts[1].trim();

            double threshold = parseThreshold(thresholdText);
            double rate = parseRate(rateText);
        }

        // Apply tiered logic: check each threshold in order
        // Return discount for the tier that the subtotal falls into
        int tierIndex = 0;
        for (int i = 0; i < tiers.length; i++) {
            if (tiers[i] == null || tiers[i].trim().isEmpty()) {
                continue;
            }

            String[] parts = tiers[i].split(":", 2);
            if (parts.length != 2) {
                continue;
            }

            String thresholdText = parts[0].trim();
            double threshold = parseThreshold(thresholdText);
            String rateText = parts[1].trim();
            double rate = parseRate(rateText);

            // If subtotal is below this threshold, return this rate
            if (subtotal < threshold) {
                return rate;
            }

            // Track the last rate (for subtotal >= all thresholds)
            tierIndex = i;
        }

        // If we reach here, subtotal >= all thresholds, return the last tier's rate
        if (tierIndex < tiers.length) {
            String[] lastParts = tiers[tierIndex].split(":", 2);
            if (lastParts.length == 2) {
                return parseRate(lastParts[1].trim());
            }
        }

        return 0.0;
    }

    private static double parseThreshold(String text) {
        if (text == null) {
            return Double.POSITIVE_INFINITY;
        }
        String cleaned = text.trim().replace("£", "").replace(",", "");
        if (cleaned.isEmpty() || "INF".equalsIgnoreCase(cleaned)) {
            return Double.POSITIVE_INFINITY;
        }
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException ex) {
            return Double.POSITIVE_INFINITY;
        }
    }

    private static double parseRate(String text) {
        if (text == null) {
            return 0.0;
        }
        String cleaned = text.trim().replace("%", "");
        if (cleaned.isEmpty()) {
            return 0.0;
        }
        try {
            double rate = Double.parseDouble(cleaned);
            return rate > 1.0 ? rate / 100.0 : rate;
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private static double roundCurrency(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    // ── Turnover rows ─────────────────────────────────────────────────────────

    public List<Object[]> getTurnoverRows(Date start, Date end) {
        List<Object[]> rows = new ArrayList<>();
        Map<String, Double> salesByDate  = new LinkedHashMap<>();
        Map<String, Double> ordersByDate = new LinkedHashMap<>();

        String salesSQL  = "SELECT saleDate, SUM(totalAmount) AS total FROM SaleTransaction WHERE saleDate BETWEEN ? AND ? GROUP BY saleDate ORDER BY saleDate";
        String ordersSQL = "SELECT dateOrdered, SUM(totalCost) AS total FROM SupplierOrder WHERE dateOrdered BETWEEN ? AND ? GROUP BY dateOrdered ORDER BY dateOrdered";

        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(salesSQL)) {
                ps.setDate(1, start); ps.setDate(2, end);
                ResultSet rs = ps.executeQuery();
                while (rs.next())
                    salesByDate.put(rs.getDate("saleDate").toString(), rs.getDouble("total"));
            }
            try (PreparedStatement ps = conn.prepareStatement(ordersSQL)) {
                ps.setDate(1, start); ps.setDate(2, end);
                ResultSet rs = ps.executeQuery();
                while (rs.next())
                    ordersByDate.put(rs.getDate("dateOrdered").toString(), rs.getDouble("total"));
            }
        } catch (SQLException e) { e.printStackTrace(); }

        java.util.Set<String> dates = new java.util.TreeSet<>();
        dates.addAll(salesByDate.keySet());
        dates.addAll(ordersByDate.keySet());
        for (String date : dates) {
            rows.add(new Object[]{
                    date,
                    String.format("%.2f", salesByDate.getOrDefault(date, 0.0)),
                    String.format("%.2f", ordersByDate.getOrDefault(date, 0.0))
            });
        }
        return rows;
    }

    // ── Queries used by panels ────────────────────────────────────────────────

    public List<Object[]> getAllSaleTransactions() {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT transactionID, saleDate, customerType, totalAmount FROM SaleTransaction ORDER BY saleDate DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) rows.add(new Object[]{
                    rs.getString("transactionID"),
                    rs.getDate("saleDate"),
                    rs.getString("customerType"),
                    String.format("%.2f", rs.getDouble("totalAmount"))
            });
        } catch (SQLException e) { e.printStackTrace(); }
        return rows;
    }

    public List<Object[]> getCustomerOrderRows(String customerNameOrId) {
        List<Object[]> rows = new ArrayList<>();
        if (customerNameOrId == null || customerNameOrId.trim().isEmpty()) return rows;
        String sql = """
            SELECT l.itemName, st.saleDate, lst.quantitySold
            FROM SaleTransaction st
            JOIN LocalStockItem_SaleTransaction lst ON st.transactionID = lst.transactionID
            JOIN LocalStockItem l ON l.itemID = lst.itemID
            WHERE st.customerType = 'account'
              AND (st.customerName = ?
                OR st.customerID = (SELECT customerID FROM CustomerAccount WHERE name = ? LIMIT 1)
                OR st.customerID = ?)
            ORDER BY st.saleDate DESC
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String n = customerNameOrId.trim();
            ps.setString(1, n); ps.setString(2, n); ps.setString(3, n);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) rows.add(new Object[]{
                    rs.getString("itemName"),
                    rs.getDate("saleDate"),
                    rs.getInt("quantitySold")
            });
        } catch (SQLException e) { e.printStackTrace(); }
        return rows;
    }

    public List<OrdersDB.OrderItemDetail> getSaleItemsByOrderId(String transactionId) {
        List<OrdersDB.OrderItemDetail> details = new ArrayList<>();
        if (transactionId == null || transactionId.trim().isEmpty()) return details;
        String sql = """
            SELECT lst.itemID, COALESCE(l.itemName, lst.itemID) AS itemName, lst.quantitySold
            FROM LocalStockItem_SaleTransaction lst
            LEFT JOIN LocalStockItem l ON l.itemID = lst.itemID
            WHERE lst.transactionID = ? ORDER BY lst.itemID
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transactionId.trim());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) details.add(new OrdersDB.OrderItemDetail(
                    rs.getString("itemID"),
                    rs.getString("itemName"),
                    rs.getInt("quantitySold")));
        } catch (SQLException e) { e.printStackTrace(); }
        return details;
    }
}