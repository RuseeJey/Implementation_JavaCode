package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MerchantDB {

    public static class MerchantRow {
        private final String merchantId;
        private final String accountHolderName;
        private final String contactName;
        private final String address;
        private final String phone;
        private final String status;
        private final double outstandingBalance;
        private final double creditLimit;
        private final String discountType;
        private final double fixedDiscount;
        private final double flexDiscount;
        private final int flexVolume;
        private final String merchUsernames;
        private final String merchLogins;

        public MerchantRow(String merchantId,
                           String accountHolderName,
                           String contactName,
                           String address,
                           String phone,
                           String status,
                           double outstandingBalance,
                           double creditLimit,
                           String discountType,
                           double fixedDiscount,
                           double flexDiscount,
                           int flexVolume,
                           String merchUsernames,
                           String merchLogins) {
            this.merchantId = merchantId;
            this.accountHolderName = accountHolderName;
            this.contactName = contactName;
            this.address = address;
            this.phone = phone;
            this.status = status;
            this.outstandingBalance = outstandingBalance;
            this.creditLimit = creditLimit;
            this.discountType = discountType;
            this.fixedDiscount = fixedDiscount;
            this.flexDiscount = flexDiscount;
            this.flexVolume = flexVolume;
            this.merchUsernames = merchUsernames;
            this.merchLogins = merchLogins;
        }

        public String getMerchantId() {
            return merchantId;
        }

        public String getAccountHolderName() {
            return accountHolderName;
        }

        public String getContactName() {
            return contactName;
        }

        public String getAddress() {
            return address;
        }

        public String getPhone() {
            return phone;
        }

        public String getStatus() {
            return status;
        }

        public double getOutstandingBalance() {
            return outstandingBalance;
        }

        public double getCreditLimit() {
            return creditLimit;
        }

        public String getDiscountType() {
            return discountType;
        }

        public double getFixedDiscount() {
            return fixedDiscount;
        }

        public double getFlexDiscount() {
            return flexDiscount;
        }

        public int getFlexVolume() {
            return flexVolume;
        }

        public String getMerchUsernames() {
            return merchUsernames;
        }

        public String getMerchLogins() {
            return merchLogins;
        }
    }

    public MerchantDB() {
        ensureTable();
        ensureDefaultCompanyRow();
    }

    private void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS Merchant (" +
                "merchant_ID VARCHAR(255) PRIMARY KEY, " +
                "account_holder_name VARCHAR(255) NOT NULL DEFAULT '', " +
                "contact_name VARCHAR(255) NOT NULL DEFAULT '', " +
                "address VARCHAR(255) DEFAULT '', " +
                "phone VARCHAR(50) DEFAULT '', " +
                "status VARCHAR(50) NOT NULL DEFAULT 'NORMAL', " +
                "outstanding_balance DOUBLE NOT NULL DEFAULT 0, " +
                "credit_limit DOUBLE NOT NULL DEFAULT 5000, " +
                "discount_type VARCHAR(20) NOT NULL DEFAULT 'NONE', " +
                "fixed_discount DOUBLE NOT NULL DEFAULT 0, " +
                "flex_discount DOUBLE NOT NULL DEFAULT 0, " +
                "flex_volume INT NOT NULL DEFAULT 0, " +
                "merch_usernames VARCHAR(255) NOT NULL DEFAULT 'NONE', " +
                "merch_logins VARCHAR(255) NOT NULL DEFAULT 'NONE')";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void ensureDefaultCompanyRow() {
        String sql = "INSERT IGNORE INTO Merchant (merchant_ID, account_holder_name, contact_name, address, phone, status, " +
                "outstanding_balance, credit_limit, discount_type, fixed_discount, flex_discount, flex_volume, " +
                "merch_usernames, merch_logins) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "MerchantID");
            stmt.setString(2, "Account Name");
            stmt.setString(3, "Contact Name");
            stmt.setString(4, "Address");
            stmt.setString(5, "Phone Number");
            stmt.setString(6, "Normal");
            stmt.setDouble(7, 0);
            stmt.setDouble(8, 0);
            stmt.setString(9, "NONE");
            stmt.setDouble(10, 0);
            stmt.setDouble(11, 0);
            stmt.setInt(12, 0);
            stmt.setString(13, "username");
            stmt.setString(14, "password");
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public MerchantRow getCompanyAccount() {
        String sql = "SELECT merchant_ID, account_holder_name, contact_name, address, phone, status, " +
                "outstanding_balance, credit_limit, discount_type, fixed_discount, flex_discount, flex_volume, " +
                "merch_usernames, merch_logins " +
                "FROM Merchant WHERE merch_usernames = 'cosymed' ORDER BY merchant_ID LIMIT 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return toRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean updateMyAccount(MerchantRow row) {
        String sql = "UPDATE Merchant SET account_holder_name = ?, contact_name = ?, address = ?, phone = ?, " +
                "status = ?, outstanding_balance = ?, credit_limit = ?, discount_type = ?, fixed_discount = ?, " +
                "flex_discount = ?, flex_volume = ?, merch_usernames = ?, merch_logins = ? WHERE merchant_ID = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, row.getAccountHolderName());
            stmt.setString(2, row.getContactName());
            stmt.setString(3, row.getAddress());
            stmt.setString(4, row.getPhone());
            stmt.setString(5, row.getStatus());
            stmt.setDouble(6, row.getOutstandingBalance());
            stmt.setDouble(7, row.getCreditLimit());
            stmt.setString(8, row.getDiscountType());
            stmt.setDouble(9, row.getFixedDiscount());
            stmt.setDouble(10, row.getFlexDiscount());
            stmt.setInt(11, row.getFlexVolume());
            stmt.setString(12, row.getMerchUsernames());
            stmt.setString(13, row.getMerchLogins());
            stmt.setString(14, row.getMerchantId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private MerchantRow toRow(ResultSet rs) throws SQLException {
        return new MerchantRow(
                rs.getString("merchant_ID"),
                rs.getString("account_holder_name"),
                rs.getString("contact_name"),
                rs.getString("address"),
                rs.getString("phone"),
                rs.getString("status"),
                rs.getDouble("outstanding_balance"),
                rs.getDouble("credit_limit"),
                rs.getString("discount_type"),
                rs.getDouble("fixed_discount"),
                rs.getDouble("flex_discount"),
                rs.getInt("flex_volume"),
                rs.getString("merch_usernames"),
                rs.getString("merch_logins")
        );
    }
}

