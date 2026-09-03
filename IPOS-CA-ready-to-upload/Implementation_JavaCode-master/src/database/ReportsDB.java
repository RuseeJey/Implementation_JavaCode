package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportsDB {

    public static class ReportResult {
        private final String[] columns;
        private final List<Object[]> rows;
        private final String summary;
        public ReportResult(String[] columns, List<Object[]> rows, String summary) {
            this.columns = columns; this.rows = rows; this.summary = summary;
        }
        public String[] getColumns()    { return columns; }
        public List<Object[]> getRows() { return rows; }
        public String getSummary()      { return summary; }
    }

    public ReportResult getTurnoverReport(String startDate, String endDate) {
        Date start = Date.valueOf(startDate);
        Date end   = Date.valueOf(endDate);
        List<Object[]> rows = new SalesDB().getTurnoverRows(start, end);
        double salesTotal = 0.0, orderTotal = 0.0;
        for (Object[] row : rows) {
            salesTotal  += Double.parseDouble(row[1].toString());
            orderTotal  += Double.parseDouble(row[2].toString());
        }
        String summary = "Turnover Report\nPeriod: " + startDate + " to " + endDate +
                "\n\nTotal sales to customers: £" + String.format("%.2f", salesTotal) +
                "\nTotal orders placed with SA: £" + String.format("%.2f", orderTotal);
        return new ReportResult(
                new String[]{"Date", "Sales to Customers (£)", "Orders to SA (£)"},
                rows, summary);
    }

    public ReportResult getStockReport(String startDate, String endDate) {
        List<Object[]> rows = new ArrayList<>();
        String sql = """
            SELECT itemID, itemName, quantity, minimumStockLevel,
                   wholesaleCost, markupRate, vatRate,
                   (wholesaleCost * (1 + markupRate/100)) AS retailPrice,
                   (wholesaleCost * (1 + markupRate/100) * quantity) AS stockValue
            FROM LocalStockItem
            ORDER BY itemID
            """;
        int lowStock = 0;
        double totalValue = 0.0;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int qty      = rs.getInt("quantity");
                int minLevel = rs.getInt("minimumStockLevel");
                double sv    = rs.getDouble("stockValue");
                totalValue  += sv;
                if (qty < minLevel) lowStock++;
                rows.add(new Object[]{
                        rs.getString("itemID"),
                        rs.getString("itemName"),
                        String.valueOf(qty),
                        String.valueOf(minLevel),
                        String.format("%.2f", sv),
                        qty < minLevel ? "LOW STOCK" : "OK"
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }

        String summary = "Stock Report\n\nItems listed: " + rows.size() +
                "\nLow stock items: " + lowStock +
                "\nEstimated stock value: £" + String.format("%.2f", totalValue);
        return new ReportResult(
                new String[]{"Item ID", "Item Name", "Qty", "Min Level", "Stock Value (£)", "Status"},
                rows, summary);
    }

    public ReportResult getDebtAnalysisReport(String startDate, String endDate) {
        double closingDebt = 0.0, paymentsReceived = 0.0, newDebtInPeriod = 0.0;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COALESCE(SUM(currentBalance),0) AS totalDebt FROM CustomerAccount");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) closingDebt = rs.getDouble("totalDebt");
        } catch (SQLException e) { e.printStackTrace(); }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COALESCE(SUM(amount),0) AS totalPaid FROM CustomerPayment WHERE paymentDate BETWEEN ? AND ?")) {
            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(endDate));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) paymentsReceived = rs.getDouble("totalPaid");
        } catch (SQLException e) { e.printStackTrace(); }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COALESCE(SUM(totalAmount),0) AS newDebt FROM SaleTransaction WHERE customerType='account' AND saleDate BETWEEN ? AND ?")) {
            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(endDate));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) newDebtInPeriod = rs.getDouble("newDebt");
        } catch (SQLException e) { e.printStackTrace(); }

        double openingDebt = Math.max(0, closingDebt - newDebtInPeriod + paymentsReceived);

        // Per-customer breakdown
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"--- SUMMARY ---", "", "", "", ""});
        rows.add(new Object[]{"Debt at start of period", "", "", "", String.format("%.2f", openingDebt)});
        rows.add(new Object[]{"New debt accumulated",    "", String.format("%.2f", newDebtInPeriod), "", ""});
        rows.add(new Object[]{"Payments received",       "", "", String.format("%.2f", paymentsReceived), ""});
        rows.add(new Object[]{"Debt at end of period",   "", "", "", String.format("%.2f", closingDebt)});
        rows.add(new Object[]{"--- PER CUSTOMER ---", "", "", "", ""});

        String perSQL = """
            SELECT ca.name, ca.accountStatus, ca.currentBalance,
                   COALESCE((SELECT SUM(cp.amount) FROM CustomerPayment cp
                              WHERE cp.customerID = ca.customerID
                              AND cp.paymentDate BETWEEN ? AND ?), 0) AS payments,
                   COALESCE((SELECT SUM(st.totalAmount) FROM SaleTransaction st
                              WHERE st.customerID = ca.customerID
                              AND st.customerType = 'account'
                              AND st.saleDate BETWEEN ? AND ?), 0) AS newDebt
            FROM CustomerAccount ca ORDER BY ca.name
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(perSQL)) {
            ps.setDate(1, Date.valueOf(startDate)); ps.setDate(2, Date.valueOf(endDate));
            ps.setDate(3, Date.valueOf(startDate)); ps.setDate(4, Date.valueOf(endDate));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new Object[]{
                        rs.getString("name"),
                        rs.getString("accountStatus"),
                        String.format("%.2f", rs.getDouble("newDebt")),
                        String.format("%.2f", rs.getDouble("payments")),
                        String.format("%.2f", rs.getDouble("currentBalance"))
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }

        String summary = "Debt Analysis Report\nPeriod: " + startDate + " to " + endDate +
                "\n\nOpening debt: £" + String.format("%.2f", openingDebt) +
                "\nPayments received: £" + String.format("%.2f", paymentsReceived) +
                "\nClosing debt: £" + String.format("%.2f", closingDebt);

        return new ReportResult(
                new String[]{"Customer / Metric", "Status", "New Debt (£)", "Payments (£)", "Balance (£)"},
                rows, summary);
    }
}