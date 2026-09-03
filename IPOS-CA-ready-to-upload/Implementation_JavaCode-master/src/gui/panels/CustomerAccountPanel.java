package gui.panels;

import IPOS_CA_CUST.CustomerAccount;
import database.CustomerAccountDB;
import database.SalesDB;
import gui.dialogs.FlexibleDiscountDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class CustomerAccountPanel extends JPanel {

    private final JTable customerTable;
    private final DefaultTableModel tableModel;
    private final JTextField customerIdField;
    private final JTextField nameField;
    private final JTextField addressField;
    private final JTextField phoneField;
    private final JTextField creditLimitField;
    private final JTextField debtField;
    private final JComboBox<String> statusCombo;
    private String discountPlanSpec = "";
    private final CustomerAccountDB customerDAO;
    private final SalesDB salesDB;
    private final String role;
    private final boolean isManager;

    public CustomerAccountPanel(String role) {
        this.role = role == null ? "" : role.toLowerCase();
        this.isManager = "manager".equals(this.role);
        customerDAO = new CustomerAccountDB();
        salesDB = new SalesDB();

        setLayout(new BorderLayout(10, 10));

        // ── Form ─────────────────────────────────────────────────────────────
        JPanel formPanel = new JPanel(new GridLayout(4, 6, 8, 8));
        formPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));

        formPanel.add(new JLabel("Customer ID:"));
        customerIdField = new JTextField(); customerIdField.setEditable(false);
        formPanel.add(customerIdField);

        formPanel.add(new JLabel("Name:"));
        nameField = new JTextField();
        formPanel.add(nameField);

        formPanel.add(new JLabel("Address:"));
        addressField = new JTextField();
        formPanel.add(addressField);

        formPanel.add(new JLabel("Phone:"));
        phoneField = new JTextField();
        formPanel.add(phoneField);

        formPanel.add(new JLabel("Credit Limit (£):"));
        creditLimitField = new JTextField();
        formPanel.add(creditLimitField);

        formPanel.add(new JLabel("Balance (£):"));
        debtField = new JTextField("0.00"); debtField.setEditable(false);
        formPanel.add(debtField);

        formPanel.add(new JLabel("Status:"));
        statusCombo = new JComboBox<>(new String[]{"normal", "suspended", "in default"});
        statusCombo.setEnabled(isManager);
        formPanel.add(statusCombo);

        add(formPanel, BorderLayout.NORTH);

        // ── Table ─────────────────────────────────────────────────────────────
        String[] cols = {"Customer ID","Name","Address","Phone","Credit Limit","Balance","Status","Discount Plan","1st Reminder","2nd Reminder"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        customerTable = new JTable(tableModel);
        add(new JScrollPane(customerTable), BorderLayout.CENTER);

        // ── Buttons ───────────────────────────────────────────────────────────
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));

        JButton addBtn      = new JButton("Add Account");
        JButton updateBtn   = new JButton("Update Account");
        JButton deleteBtn   = new JButton("Delete Account");
        JButton payBtn      = new JButton("Record Payment");
        JButton stmtBtn     = new JButton("View Statement");
        JButton reactBtn    = new JButton("Reactivate Account");
        JButton discountBtn  = new JButton("Set Discount");
        JButton refreshBtn  = new JButton("Refresh");

        // Reactivate is Manager-only
        reactBtn.setEnabled(isManager);
        // Discount button is manager-only
        discountBtn.setEnabled(isManager);

        buttonPanel.add(addBtn);
        buttonPanel.add(updateBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(payBtn);
        buttonPanel.add(stmtBtn);
        buttonPanel.add(reactBtn);
        buttonPanel.add(discountBtn);
        buttonPanel.add(refreshBtn);

        add(buttonPanel, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> addCustomer());
        updateBtn.addActionListener(e -> updateCustomer());
        deleteBtn.addActionListener(e -> deleteCustomer());
        payBtn.addActionListener(e -> recordPayment());
        stmtBtn.addActionListener(e -> generateStatement());
        reactBtn.addActionListener(e -> reactivateAccount());
        discountBtn.addActionListener(e -> setDiscount());
        refreshBtn.addActionListener(e -> loadCustomerData());

        customerTable.getSelectionModel().addListSelectionListener(e -> fillFieldsFromSelectedRow());

        loadCustomerData();
    }

    // Backward-compatible constructor (no role)
    public CustomerAccountPanel() { this("pharmacist"); }

    private void loadCustomerData() {
        tableModel.setRowCount(0);
        for (CustomerAccount ca : customerDAO.getAllCustomers()) {
            tableModel.addRow(new Object[]{
                    ca.getCustomerID(), ca.getName(), ca.getAddress(), ca.getPhone(),
                    String.format("%.2f", ca.getCreditLimit()),
                    String.format("%.2f", ca.getCurrentBalance()),
                    ca.getAccountStatus(),
                    ca.getDiscountPlan() != null ? ca.getDiscountPlan() : "",
                    ca.getStatus1stReminder() != null ? ca.getStatus1stReminder() : "",
                    ca.getStatus2ndReminder() != null ? ca.getStatus2ndReminder() : ""
            });
        }
    }

    private void addCustomer() {
        String name = nameField.getText().trim();
        String addr = addressField.getText().trim();
        String creditText = creditLimitField.getText().trim();

        if (name.isEmpty() || addr.isEmpty() || creditText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name, address and credit limit are required.");
            return;
        }
        try {
            double credit = Double.parseDouble(creditText);

            CustomerAccount ca = new CustomerAccount(name, addr, credit);
            ca.setPhone(phoneField.getText().trim());
            ca.setDiscountPlan(resolveDiscountPlanForSave());

            if (customerDAO.addCustomer(ca)) {
                loadCustomerData();
                clearFields();
                JOptionPane.showMessageDialog(this, "Customer added successfully.");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to add customer.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Credit limit must be a number.");
        }
    }

    private void updateCustomer() {
        int row = customerTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a customer first."); return; }

        try {
            String selectedStatus = isManager
                    ? (String) statusCombo.getSelectedItem()
                    : tableModel.getValueAt(row, 6).toString();

            CustomerAccount ca = new CustomerAccount(
                    customerIdField.getText().trim(), nameField.getText().trim(),
                    addressField.getText().trim(), Double.parseDouble(creditLimitField.getText().trim()),
                    Double.parseDouble(debtField.getText().trim()),
                    selectedStatus,
                    resolveDiscountPlanForSave());
            ca.setPhone(phoneField.getText().trim());
            ca.setStatus1stReminder(tableModel.getValueAt(row, 8).toString());
            ca.setStatus2ndReminder(tableModel.getValueAt(row, 9).toString());

            if (customerDAO.updateCustomer(ca)) {
                loadCustomerData();
                JOptionPane.showMessageDialog(this, "Customer updated successfully.");
            } else {
                JOptionPane.showMessageDialog(this, "Update failed.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Credit limit must be a number.");
        }
    }

    private void deleteCustomer() {
        int row = customerTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a customer first."); return; }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete this customer account?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        if (customerDAO.deleteCustomer(customerIdField.getText().trim())) {
            tableModel.removeRow(row);
            clearFields();
        } else {
            JOptionPane.showMessageDialog(this, "Delete failed.");
        }
    }

    private void recordPayment() {
        int row = customerTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a customer first."); return; }

        String amountStr = JOptionPane.showInputDialog(this, "Enter payment amount (£):");
        if (amountStr == null || amountStr.trim().isEmpty()) return;

        try {
            double amount = Double.parseDouble(amountStr.trim());

            // Account holders must pay by card
            String[] methods = {"Credit Card", "Debit Card"};
            String method = (String) JOptionPane.showInputDialog(this,
                    "Payment method:", "Payment Method",
                    JOptionPane.QUESTION_MESSAGE, null, methods, methods[0]);
            if (method == null) return;

            String cardType  = JOptionPane.showInputDialog(this, "Card type (Visa/Mastercard/Amex):");
            String first4    = JOptionPane.showInputDialog(this, "First 4 digits of card:");
            String last4     = JOptionPane.showInputDialog(this, "Last 4 digits of card:");
            String expiry    = JOptionPane.showInputDialog(this, "Expiry (MM/YYYY):");

            String customerId = customerIdField.getText().trim();
            boolean ok = customerDAO.makePayment(customerId, amount, "card",
                    cardType, first4, last4, expiry);

            if (ok) {
                loadCustomerData();
                JOptionPane.showMessageDialog(this, "Payment of £" +
                        String.format("%.2f", amount) + " recorded.");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to record payment.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid amount.");
        }
    }

    private void reactivateAccount() {
        int row = customerTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a customer first."); return; }

        String status = tableModel.getValueAt(row, 6).toString();
        if (!"in default".equals(status)) {
            JOptionPane.showMessageDialog(this, "Account is not in default.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Reactivate this account from 'in default' to 'normal'?",
                "Confirm Reactivation", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        if (customerDAO.reactivateAccount(customerIdField.getText().trim())) {
            loadCustomerData();
            JOptionPane.showMessageDialog(this, "Account reactivated successfully.");
        } else {
            JOptionPane.showMessageDialog(this, "Reactivation failed.");
        }
    }

    private void generateStatement() {
        int row = customerTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a customer first."); return; }

        String name = tableModel.getValueAt(row, 1).toString();
        String debt = debtField.getText();
        List<Object[]> orders = salesDB.getCustomerOrderRows(name);

        StringBuilder sb = new StringBuilder();
        sb.append("Statement for ").append(name)
                .append("\nOutstanding balance: £").append(debt).append("\n\nPurchase history:");
        if (orders.isEmpty()) {
            sb.append("\n  No purchases found.");
        } else {
            for (Object[] r : orders)
                sb.append("\n  ").append(r[0]).append(" | Date: ").append(r[1])
                        .append(" | Qty: ").append(r[2]);
        }

        JTextArea ta = new JTextArea(sb.toString(), 18, 60);
        ta.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta),
                "Customer Statement", JOptionPane.INFORMATION_MESSAGE);
    }

    private void fillFieldsFromSelectedRow() {
        int row = customerTable.getSelectedRow();
        if (row == -1) return;
        customerIdField.setText(tableModel.getValueAt(row, 0).toString());
        nameField.setText(tableModel.getValueAt(row, 1).toString());
        addressField.setText(tableModel.getValueAt(row, 2).toString());
        phoneField.setText(tableModel.getValueAt(row, 3).toString());
        creditLimitField.setText(tableModel.getValueAt(row, 4).toString());
        debtField.setText(tableModel.getValueAt(row, 5).toString());
        statusCombo.setSelectedItem(tableModel.getValueAt(row, 6).toString());
        String plan = tableModel.getValueAt(row, 7) == null ? "" : tableModel.getValueAt(row, 7).toString().trim();
        if (plan.isEmpty()) {
            discountPlanSpec = "";
        } else if (plan.toUpperCase().startsWith("FIXED|")) {
            discountPlanSpec = plan;
        } else if (plan.toUpperCase().startsWith("FLEX|")) {
            discountPlanSpec = plan;
        } else {
            discountPlanSpec = plan;
        }
    }

    private void clearFields() {
        customerIdField.setText(""); nameField.setText(""); addressField.setText("");
        phoneField.setText(""); creditLimitField.setText(""); debtField.setText("0.00");
        statusCombo.setSelectedIndex(0);
    }

    private void setDiscount() {
        int row = customerTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a customer first.");
            return;
        }

        String[] options = {"None", "Fixed", "Flexible", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Choose a discount type for this customer.",
                "Set Discount",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == 3 || choice == JOptionPane.CLOSED_OPTION) {
            return;
        }

        if (choice == 0) {
            discountPlanSpec = "";
            String customerId = customerIdField.getText().trim();
            if (customerDAO.updateDiscountPlan(customerId, "none")) {
                loadCustomerData();
                clearFields();
                JOptionPane.showMessageDialog(this,
                        "Discount removed for this customer.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to remove discount.");
            }
            return;
        }

        if (choice == 1) {
            String input = JOptionPane.showInputDialog(this,
                    "Enter fixed discount percentage (e.g. 10 for 10%):",
                    "Fixed Discount",
                    JOptionPane.QUESTION_MESSAGE);
            if (input == null) {
                return;
            }

            try {
                double percent = Double.parseDouble(input.trim());
                if (percent < 0.0 || percent > 100.0) {
                    JOptionPane.showMessageDialog(this, "Discount percentage must be between 0 and 100.");
                    return;
                }
                discountPlanSpec = "FIXED|" + trimNumber(percent);
                String customerId = customerIdField.getText().trim();
                if (customerDAO.updateDiscountPlan(customerId, discountPlanSpec)) {
                    loadCustomerData();
                    clearFields();
                    JOptionPane.showMessageDialog(this,
                            "Fixed Discount configured:\n" + discountPlanSpec,
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to update discount plan.");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number for the discount percentage.");
            }
        } else {
            JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            FlexibleDiscountDialog dialog = new FlexibleDiscountDialog(parentFrame);
            dialog.setVisible(true);

            if (dialog.isConfirmed()) {
                String newDiscountPlan = dialog.getDiscountString();
                discountPlanSpec = newDiscountPlan;
                String customerId = customerIdField.getText().trim();
                if (customerDAO.updateDiscountPlan(customerId, discountPlanSpec)) {
                    loadCustomerData();
                    clearFields();
                    JOptionPane.showMessageDialog(this,
                            "Flexible Discount configured:\n" + newDiscountPlan,
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to update discount plan.");
                }
            }
        }
    }

    private String resolveDiscountPlanForSave() {
        if (discountPlanSpec == null || discountPlanSpec.trim().isEmpty()) {
            return "none";
        }
        return discountPlanSpec.trim();
    }

    private String trimNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}