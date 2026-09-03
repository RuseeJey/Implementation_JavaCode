package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TemplatesDB {

	public static class TemplateSettings {
		private final String pharmacyName;
		private final String logoPath;
		private final String address;
		private final String email;
		private final String reminderTemplate;
		private final String invoiceTemplate;

		public TemplateSettings(String pharmacyName, String logoPath, String address, String email, String reminderTemplate, String invoiceTemplate) {
			this.pharmacyName = pharmacyName;
			this.logoPath = logoPath;
			this.address = address;
			this.email = email;
			this.reminderTemplate = reminderTemplate;
			this.invoiceTemplate = invoiceTemplate;
		}

		public String getPharmacyName() { return pharmacyName; }
		public String getLogoPath() { return logoPath; }
		public String getAddress() { return address; }
		public String getEmail() { return email; }
		public String getReminderTemplate() { return reminderTemplate; }
		public String getInvoiceTemplate() { return invoiceTemplate; }
	}

	public TemplatesDB() {
		ensureTable();
	}

	private void ensureTable() {
		String sql = "CREATE TABLE IF NOT EXISTS GuiTemplateSettings (" +
				"id INT PRIMARY KEY, pharmacyName VARCHAR(255) NOT NULL, logoPath VARCHAR(255) NOT NULL, " +
				"address VARCHAR(255) NOT NULL, email VARCHAR(255) NOT NULL, reminderTemplate TEXT NOT NULL, invoiceTemplate TEXT NOT NULL)";
		try (Connection conn = DatabaseManager.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public TemplateSettings defaultSettings() {
		return new TemplateSettings(
				"Cosymed Ltd",
				"logo.png",
				"25, Bond Street, London WC1V 8LS",
				"cosymed@example.com",
				"Dear {customerName},\n\nREMINDER - INVOICE NO.: {invoiceNo}\nIPOS Account: {accountNo}   Total Amount: {amount}\n\nAccording to our records, it appears that we have not yet received payment of the above invoice, which was raised against {customerName} on {invoiceDate}, for purchasing pharmaceutical goods from Cosymed Ltd.\n\nWe would appreciate payment in full by {payByDate}.\n\nIf you have already sent a payment to us recently, please accept our apologies.\n\nYours sincerely,\nCosymed Ltd",
				"RECEIPT / INVOICE\n\nReceipt ID: {receiptId}\nDate: {date}\nCustomer: {customerName}\nTotal Paid: {total}\n\nThank you for your purchase.\nCosymed Ltd\n25, Bond Street, London WC1V 8LS\ncosymed@example.com"
		);
	}

	public TemplateSettings loadSettings() {
		String sql = "SELECT pharmacyName, logoPath, address, email, reminderTemplate, invoiceTemplate FROM GuiTemplateSettings WHERE id = 1";
		try (Connection conn = DatabaseManager.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql);
			 ResultSet rs = stmt.executeQuery()) {
			if (rs.next()) {
				return new TemplateSettings(
						rs.getString("pharmacyName"),
						rs.getString("logoPath"),
						rs.getString("address"),
						rs.getString("email"),
						rs.getString("reminderTemplate"),
						rs.getString("invoiceTemplate")
				);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		TemplateSettings defaults = defaultSettings();
		saveSettings(defaults);
		return defaults;
	}

	public boolean saveSettings(TemplateSettings settings) {
		String sql = "INSERT INTO GuiTemplateSettings (id, pharmacyName, logoPath, address, email, reminderTemplate, invoiceTemplate) VALUES (1, ?, ?, ?, ?, ?, ?) " +
				"ON DUPLICATE KEY UPDATE pharmacyName = VALUES(pharmacyName), logoPath = VALUES(logoPath), address = VALUES(address), " +
				"email = VALUES(email), reminderTemplate = VALUES(reminderTemplate), invoiceTemplate = VALUES(invoiceTemplate)";

		try (Connection conn = DatabaseManager.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, settings.getPharmacyName());
			stmt.setString(2, settings.getLogoPath());
			stmt.setString(3, settings.getAddress());
			stmt.setString(4, settings.getEmail());
			stmt.setString(5, settings.getReminderTemplate());
			stmt.setString(6, settings.getInvoiceTemplate());
			return stmt.executeUpdate() > 0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
}

