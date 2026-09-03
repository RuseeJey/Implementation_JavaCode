package gui.panels;

import database.MerchantDB;

import javax.swing.*;
import java.awt.*;

public class CompanyAccountPanel extends JPanel {

    private final MerchantDB merchantDB;

    private final JTextField merchantIdField;
    private final JTextField usernameField;
    private final JTextField accountHolderNameField;
    private final JTextField contactNameField;
    private final JTextField addressField;
    private final JTextField phoneField;
    private final JComboBox<String> statusCombo;
    private final JTextField outstandingBalanceField;
    private final JTextField creditLimitField;
    private final JComboBox<String> discountTypeCombo;
    private final JTextField fixedDiscountField;
    private final JTextField flexDiscountField;
    private final JTextField flexVolumeField;
    private final JTextField merchLoginsField;

    public CompanyAccountPanel() {
        this.merchantDB = new MerchantDB();

        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(7, 4, 10, 10));

        formPanel.add(new JLabel("Merchant ID:"));
        merchantIdField = new JTextField();
        merchantIdField.setEditable(false);
        formPanel.add(merchantIdField);

        formPanel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        usernameField.setEditable(false);
        formPanel.add(usernameField);

        formPanel.add(new JLabel("Account Holder Name:"));
        accountHolderNameField = new JTextField();
        formPanel.add(accountHolderNameField);

        formPanel.add(new JLabel("Contact Name:"));
        contactNameField = new JTextField();
        formPanel.add(contactNameField);

        formPanel.add(new JLabel("Address:"));
        addressField = new JTextField();
        formPanel.add(addressField);

        formPanel.add(new JLabel("Phone:"));
        phoneField = new JTextField();
        formPanel.add(phoneField);

        formPanel.add(new JLabel("Status:"));
        statusCombo = new JComboBox<>(new String[]{"NORMAL", "SUSPENDED", "IN_DEFAULT"});
        formPanel.add(statusCombo);

        formPanel.add(new JLabel("Outstanding Balance:"));
        outstandingBalanceField = new JTextField("0.00");
        formPanel.add(outstandingBalanceField);

        formPanel.add(new JLabel("Credit Limit:"));
        creditLimitField = new JTextField("0.00");
        formPanel.add(creditLimitField);

        formPanel.add(new JLabel("Discount Type:"));
        discountTypeCombo = new JComboBox<>(new String[]{"NONE", "FIXED", "FLEX"});
        formPanel.add(discountTypeCombo);

        formPanel.add(new JLabel("Fixed Discount:"));
        fixedDiscountField = new JTextField("0.00");
        formPanel.add(fixedDiscountField);

        formPanel.add(new JLabel("Flex Discount:"));
        flexDiscountField = new JTextField("0.00");
        formPanel.add(flexDiscountField);

        formPanel.add(new JLabel("Flex Volume:"));
        flexVolumeField = new JTextField("0");
        formPanel.add(flexVolumeField);

        formPanel.add(new JLabel("Login Notes:"));
        merchLoginsField = new JTextField();
        formPanel.add(merchLoginsField);

        add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Update Company Account");
        JButton refreshButton = new JButton("Refresh");
        buttonPanel.add(saveButton);
        buttonPanel.add(refreshButton);
        add(buttonPanel, BorderLayout.SOUTH);

        saveButton.addActionListener(e -> updateCompanyAccount());
        refreshButton.addActionListener(e -> loadCompanyAccount());
        discountTypeCombo.addActionListener(e -> updateDiscountFieldState());

        loadCompanyAccount();
    }

    private void loadCompanyAccount() {
        MerchantDB.MerchantRow row = merchantDB.getCompanyAccount();
        if (row == null) {
            JOptionPane.showMessageDialog(this, "Cannot load company account from Merchant table.");
            return;
        }

        merchantIdField.setText(row.getMerchantId());
        usernameField.setText(row.getMerchUsernames());
        accountHolderNameField.setText(row.getAccountHolderName());
        contactNameField.setText(row.getContactName());
        addressField.setText(row.getAddress());
        phoneField.setText(row.getPhone());
        statusCombo.setSelectedItem(normalizeStatus(row.getStatus()));
        outstandingBalanceField.setText(String.format("%.2f", row.getOutstandingBalance()));
        creditLimitField.setText(String.format("%.2f", row.getCreditLimit()));
        discountTypeCombo.setSelectedItem(normalizeDiscountType(row.getDiscountType()));
        fixedDiscountField.setText(String.format("%.2f", row.getFixedDiscount()));
        flexDiscountField.setText(String.format("%.2f", row.getFlexDiscount()));
        flexVolumeField.setText(String.valueOf(row.getFlexVolume()));
        merchLoginsField.setText(row.getMerchLogins());
        updateDiscountFieldState();
    }

    private void updateCompanyAccount() {
        try {
            String merchantId = merchantIdField.getText().trim();
            double outstandingBalance = Double.parseDouble(outstandingBalanceField.getText().trim());
            double creditLimit = Double.parseDouble(creditLimitField.getText().trim());
            double fixedDiscount = Double.parseDouble(fixedDiscountField.getText().trim());
            double flexDiscount = Double.parseDouble(flexDiscountField.getText().trim());
            int flexVolume = Integer.parseInt(flexVolumeField.getText().trim());

            String accountHolderName = accountHolderNameField.getText().trim();
            String contactName = contactNameField.getText().trim();
            String address = addressField.getText().trim();
            String phone = phoneField.getText().trim();
            String status = normalizeStatus(String.valueOf(statusCombo.getSelectedItem()));
            String discountType = normalizeDiscountType(String.valueOf(discountTypeCombo.getSelectedItem()));
            String merchLogins = merchLoginsField.getText().trim();
            String merchUsernames = usernameField.getText().trim();

            if (merchantId.isEmpty() || accountHolderName.isEmpty() || contactName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Merchant ID, account holder name and contact name are required.");
                return;
            }

            MerchantDB.MerchantRow row = new MerchantDB.MerchantRow(
                    merchantId,
                    accountHolderName,
                    contactName,
                    address,
                    phone,
                    status,
                    outstandingBalance,
                    creditLimit,
                    discountType,
                    fixedDiscount,
                    flexDiscount,
                    flexVolume,
                    merchUsernames,
                    merchLogins
            );

            boolean updated = merchantDB.updateMyAccount(row);
            if (!updated) {
                JOptionPane.showMessageDialog(this, "Update failed. Please refresh and try again.");
                return;
            }

            JOptionPane.showMessageDialog(this, "CompanyAccount updated successfully.");
            loadCompanyAccount();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Outstanding, limit, discount, and volume must be valid numbers.");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "NORMAL";
        }

        String normalized = status.trim().toUpperCase();
        if ("IN DEFAULT".equals(normalized)) {
            return "IN_DEFAULT";
        }
        if (!"NORMAL".equals(normalized) && !"SUSPENDED".equals(normalized) && !"IN_DEFAULT".equals(normalized)) {
            return "NORMAL";
        }
        return normalized;
    }

    private String normalizeDiscountType(String discountType) {
        if (discountType == null || discountType.trim().isEmpty()) {
            return "NONE";
        }

        String normalized = discountType.trim().toUpperCase();
        if (!"NONE".equals(normalized) && !"FIXED".equals(normalized) && !"FLEX".equals(normalized)) {
            return "NONE";
        }
        return normalized;
    }

    private void updateDiscountFieldState() {
        String discountType = normalizeDiscountType(String.valueOf(discountTypeCombo.getSelectedItem()));
        boolean fixedMode = "FIXED".equals(discountType);
        boolean flexMode = "FLEX".equals(discountType);

        fixedDiscountField.setEnabled(fixedMode);
        fixedDiscountField.setEditable(fixedMode);

        flexDiscountField.setEnabled(flexMode);
        flexDiscountField.setEditable(flexMode);

        flexVolumeField.setEnabled(flexMode);
        flexVolumeField.setEditable(flexMode);
    }
}

