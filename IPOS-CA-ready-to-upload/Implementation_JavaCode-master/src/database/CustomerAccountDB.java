package database;

import IPOS_CA_CUST.CustomerAccount;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CustomerAccountDB {

    private CustomerAccount mapRow(ResultSet rs) throws SQLException {
        CustomerAccount ca = new CustomerAccount();
        ca.setCustomerID(rs.getString("customerID"));
        ca.setName(rs.getString("name"));
        ca.setAddress(rs.getString("address"));
        ca.setPhone(rs.getString("phone"));
        ca.setCreditLimit(rs.getDouble("creditLimit"));
        ca.setCurrentBalance(rs.getDouble("currentBalance"));
        ca.setAccountStatus(rs.getString("accountStatus"));
        ca.setPlanID(rs.getString("discountPlan"));
        ca.setStatus1stReminder(rs.getString("status1stReminder"));
        ca.setStatus2ndReminder(rs.getString("status2ndReminder"));
        Date d1 = rs.getDate("date1stReminder");
        if (d1 != null) ca.setDate1stReminder(d1.toLocalDate());
        Date d2 = rs.getDate("date2ndReminder");
        if (d2 != null) ca.setDate2ndReminder(d2.toLocalDate());
        Date lp = rs.getDate("lastPaymentDate");
        if (lp != null) ca.setLastPaymentDate(lp.toLocalDate());
        ca.setPeriodDebtCleared(rs.getString("periodDebtCleared"));
        return ca;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public List<CustomerAccount> getAllCustomers() {
        List<CustomerAccount> list = new ArrayList<>();
        String sql = "SELECT * FROM CustomerAccount ORDER BY name";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public CustomerAccount getCustomerByID(String customerID) {
        String sql = "SELECT * FROM CustomerAccount WHERE customerID = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public String getDiscountPlanByName(String customerName) {
        if (customerName == null || customerName.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT discountPlan FROM CustomerAccount WHERE name = ? LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("discountPlan") : null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean addCustomer(CustomerAccount ca) {
        String sql = """
            INSERT INTO CustomerAccount
              (customerID, name, address, phone, creditLimit, currentBalance,
               accountStatus, discountPlan, status1stReminder, status2ndReminder,
               date1stReminder, date2ndReminder, lastPaymentDate, periodDebtCleared)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,  ca.getCustomerID());
            ps.setString(2,  ca.getName());
            ps.setString(3,  ca.getAddress());
            ps.setString(4,  ca.getPhone());
            ps.setDouble(5,  ca.getCreditLimit());
            ps.setDouble(6,  ca.getCurrentBalance());
            ps.setString(7,  ca.getAccountStatus() != null ? ca.getAccountStatus() : "normal");
            ps.setString(8,  ca.getPlanID());
            ps.setString(9,  "no_need");
            ps.setString(10, "no_need");
            ps.setNull(11, Types.DATE);
            ps.setNull(12, Types.DATE);
            ps.setNull(13, Types.DATE);
            ps.setNull(14, Types.VARCHAR);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean updateCustomer(CustomerAccount ca) {
        CustomerAccount existing = getCustomerByID(ca.getCustomerID());
        if (existing == null) return false;
        String discountPlan = isBlank(ca.getPlanID()) ? existing.getPlanID() : ca.getPlanID();
        String s1     = isBlank(ca.getStatus1stReminder()) ? existing.getStatus1stReminder() : ca.getStatus1stReminder();
        String s2     = isBlank(ca.getStatus2ndReminder()) ? existing.getStatus2ndReminder() : ca.getStatus2ndReminder();

        String sql = """
            UPDATE CustomerAccount SET
              name=?, address=?, phone=?, creditLimit=?, currentBalance=?,
              accountStatus=?, discountPlan=?,
              status1stReminder=?, status2ndReminder=?
            WHERE customerID=?
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,  ca.getName());
            ps.setString(2,  ca.getAddress());
            ps.setString(3,  ca.getPhone());
            ps.setDouble(4,  ca.getCreditLimit());
            ps.setDouble(5,  ca.getCurrentBalance());
            ps.setString(6,  ca.getAccountStatus());
            ps.setString(7,  discountPlan);
            ps.setString(8,  s1 != null ? s1 : "no_need");
            ps.setString(9,  s2 != null ? s2 : "no_need");
            ps.setString(10, ca.getCustomerID());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean deleteCustomer(String customerID) {
        String sql = "DELETE FROM CustomerAccount WHERE customerID = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean updateDiscountPlan(String customerID, String discountPlan) {
        String sql = "UPDATE CustomerAccount SET discountPlan=? WHERE customerID=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, discountPlan != null ? discountPlan : "none");
            ps.setString(2, customerID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // ── Balance ───────────────────────────────────────────────────────────────

    public boolean addToBalance(String customerID, double amount) {
        String sql = "UPDATE CustomerAccount SET currentBalance = currentBalance + ? WHERE customerID = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setString(2, customerID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // ── Payment — writes to CustomerPayment AND ca_card_payments ─────────────

    public boolean makePayment(String customerID, double amount,
                               String paymentMethod,
                               String cardType, String cardFirst4,
                               String cardLast4, String cardExpiry) {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            String reference = "PAY-" + System.currentTimeMillis();

            // 1. Insert into our internal CustomerPayment table
            String paySQL = """
                INSERT INTO CustomerPayment
                  (paymentID, customerID, paymentDate, amount,
                   paymentMethod, cardType, cardFirst4, cardLast4, cardExpiry)
                VALUES (?,?,CURDATE(),?,?,?,?,?,?)
                """;
            try (PreparedStatement ps = conn.prepareStatement(paySQL)) {
                ps.setString(1, reference);
                ps.setString(2, customerID);
                ps.setDouble(3, amount);
                ps.setString(4, paymentMethod);
                ps.setString(5, cardType);
                ps.setString(6, cardFirst4);
                ps.setString(7, cardLast4);
                ps.setString(8, cardExpiry);
                ps.executeUpdate();
            }

            // 2. Insert into shared ca_card_payments table (PU team reads this)
            // Note: their schema only has card_last4, not card_first4
            String sharedPaySQL = """
                INSERT INTO ca_card_payments
                  (reference, payee, amount, card_type, card_last4, expiry, status, note)
                VALUES (?,?,?,?,?,?,?,?)
                """;
            try (PreparedStatement ps = conn.prepareStatement(sharedPaySQL)) {
                String note = paymentMethod == null || paymentMethod.trim().isEmpty()
                        ? null : "method=" + paymentMethod;
                ps.setString(1, reference);
                ps.setString(2, customerID);
                ps.setDouble(3, amount);
                ps.setString(4, cardType);
                ps.setString(5, cardLast4);
                ps.setString(6, cardExpiry);
                ps.setString(7, "SUCCESS");
                ps.setString(8, note);
                ps.executeUpdate();
            }

            // 3. Reduce balance and update lastPaymentDate
            String balSQL = """
                UPDATE CustomerAccount
                SET currentBalance = currentBalance - ?,
                    lastPaymentDate = CURDATE()
                WHERE customerID = ?
                """;
            try (PreparedStatement ps = conn.prepareStatement(balSQL)) {
                ps.setDouble(1, amount);
                ps.setString(2, customerID);
                ps.executeUpdate();
            }

            // 4. If not in default and balance now cleared, reset status
            String checkSQL = "SELECT accountStatus, currentBalance FROM CustomerAccount WHERE customerID = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkSQL)) {
                ps.setString(1, customerID);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String status  = rs.getString("accountStatus");
                    double balance = rs.getDouble("currentBalance");
                    if (!"in default".equals(status) && balance <= 0.0) {
                        String resetSQL = """
                            UPDATE CustomerAccount
                            SET accountStatus = 'normal',
                                status1stReminder = 'no_need',
                                status2ndReminder = 'no_need',
                                date1stReminder = NULL,
                                date2ndReminder = NULL
                            WHERE customerID = ?
                            """;
                        try (PreparedStatement rps = conn.prepareStatement(resetSQL)) {
                            rps.setString(1, customerID);
                            rps.executeUpdate();
                        }
                    }
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean makePayment(String customerID, double amount) {
        return makePayment(customerID, amount, "cash", null, null, null, null);
    }

    // ── Status engine ─────────────────────────────────────────────────────────

    public void runStatusEngine(String customerID) {
        CustomerAccount ca = getCustomerByID(customerID);
        if (ca == null) return;
        if ("in default".equals(ca.getAccountStatus())) return;

        if (ca.getCurrentBalance() <= 0.0) {
            if (!"normal".equals(ca.getAccountStatus())) {
                setStatus(customerID, "normal");
                resetReminderFlags(customerID);
            }
            return;
        }

        LocalDate today             = LocalDate.now();
        LocalDate suspendThreshold  = today.withDayOfMonth(15);
        LocalDate defaultThreshold  = today.withDayOfMonth(1).plusMonths(1).minusDays(1);

        if (today.isAfter(defaultThreshold)) {
            if (!"in default".equals(ca.getAccountStatus())) {
                setStatus(customerID, "in default");
                String sql = "UPDATE CustomerAccount SET status2ndReminder='due', date2ndReminder=? WHERE customerID=?";
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setObject(1, today);
                    ps.setString(2, customerID);
                    ps.executeUpdate();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        } else if (today.isAfter(suspendThreshold) || today.isEqual(suspendThreshold)) {
            if ("normal".equals(ca.getAccountStatus())) {
                setStatus(customerID, "suspended");
                String sql = "UPDATE CustomerAccount SET status1stReminder='due' WHERE customerID=?";
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, customerID);
                    ps.executeUpdate();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    public void runStatusEngineForAll() {
        getAllCustomers().forEach(ca -> runStatusEngine(ca.getCustomerID()));
    }

    public boolean reactivateAccount(String customerID) {
        String sql = """
            UPDATE CustomerAccount
            SET accountStatus = 'normal',
                status1stReminder = 'no_need',
                status2ndReminder = 'no_need',
                date1stReminder = NULL,
                date2ndReminder = NULL
            WHERE customerID = ? AND accountStatus = 'in default'
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean mark1stReminderSent(String customerID) {
        LocalDate schedule2nd = LocalDate.now().plusDays(15);
        String sql = """
            UPDATE CustomerAccount
            SET status1stReminder = 'sent',
                status2ndReminder = 'due',
                date2ndReminder = ?
            WHERE customerID = ?
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, schedule2nd);
            ps.setString(2, customerID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean mark2ndReminderSent(String customerID) {
        String sql = "UPDATE CustomerAccount SET status2ndReminder='sent' WHERE customerID=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public List<CustomerAccount> getAccountsDue1stReminder() {
        List<CustomerAccount> list = new ArrayList<>();
        String sql = "SELECT * FROM CustomerAccount WHERE status1stReminder='due' AND currentBalance>0 ORDER BY name";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<CustomerAccount> getAccountsDue2ndReminder() {
        List<CustomerAccount> list = new ArrayList<>();
        String sql = "SELECT * FROM CustomerAccount WHERE status2ndReminder='due' AND currentBalance>0 ORDER BY name";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private void setStatus(String customerID, String status) {
        String sql = "UPDATE CustomerAccount SET accountStatus=? WHERE customerID=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, customerID);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void resetReminderFlags(String customerID) {
        String sql = """
            UPDATE CustomerAccount
            SET status1stReminder='no_need', status2ndReminder='no_need',
                date1stReminder=NULL, date2ndReminder=NULL
            WHERE customerID=?
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerID);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private boolean isBlank(String v) { return v == null || v.trim().isEmpty(); }
}