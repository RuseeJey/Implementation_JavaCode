package database;

import IPOS_CA_CUST.CustomerAccount;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RemindersDB {

    public static class ReminderRow {
        private final CustomerAccount customerAccount;
        private final String reminderStatus;
        private final String dueDate;

        public ReminderRow(CustomerAccount ca, String reminderStatus, String dueDate) {
            this.customerAccount = ca;
            this.reminderStatus  = reminderStatus;
            this.dueDate         = dueDate;
        }
        public String getCustomerId()    { return customerAccount.getCustomerID(); }
        public String getCustomerName()  { return customerAccount.getName(); }
        public double getBalance()       { return customerAccount.getCurrentBalance(); }
        public String getAccountStatus() { return customerAccount.getAccountStatus(); }
        public String getReminderStatus(){ return reminderStatus; }
        public String getDueDate()       { return dueDate; }
    }

    /**
     * Load accounts that have a reminder due.
     * Type must be "1st Reminder" or "2nd Reminder" (matches the combo box labels).
     */
    public List<ReminderRow> loadOverdueAccounts(String reminderTypeLabel) {
        List<ReminderRow> rows = new ArrayList<>();
        boolean isFirst = "1st Reminder".equals(reminderTypeLabel);

        String sql = isFirst
                ? "SELECT * FROM CustomerAccount WHERE status1stReminder = 'due' AND currentBalance > 0 ORDER BY name"
                : "SELECT * FROM CustomerAccount WHERE status2ndReminder = 'due' AND currentBalance > 0 ORDER BY name";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                CustomerAccount ca = new CustomerAccount(
                        rs.getString("customerID"), rs.getString("name"),
                        rs.getString("address"),    rs.getDouble("creditLimit"),
                        rs.getDouble("currentBalance"), rs.getString("accountStatus"));

                LocalDate payBy = LocalDate.now().plusDays(isFirst ? 7 : 15);
                rows.add(new ReminderRow(ca, "due", payBy.toString()));
            }
        } catch (SQLException e) { e.printStackTrace(); }

        return rows;
    }

    /**
     * Mark reminder as sent and update the reminder flags in CustomerAccount.
     * For 1st reminder: schedule 2nd reminder for today + 15 days.
     * For 2nd reminder: mark as sent.
     */
    public boolean markReminderAsSent(String customerId, String reminderTypeLabel) {
        boolean isFirst = "1st Reminder".equals(reminderTypeLabel);

        // Log it
        String logSQL = "INSERT INTO GuiReminderLog (customerID, reminderType, generatedAt, payByDate) VALUES (?,?,CURDATE(),?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(logSQL)) {
            ps.setString(1, customerId);
            ps.setString(2, isFirst ? "1st" : "2nd");
            ps.setObject(3, LocalDate.now().plusDays(7));
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }

        // Update flags in CustomerAccount
        CustomerAccountDB caDB = new CustomerAccountDB();
        if (isFirst) {
            return caDB.mark1stReminderSent(customerId);
        } else {
            return caDB.mark2ndReminderSent(customerId);
        }
    }
}