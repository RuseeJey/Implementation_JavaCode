package gui.panels;

import database.RemindersDB;
import database.TemplatesDB;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class RemindersPanel extends JPanel {

    private final JTable overdueTable;
    private final DefaultTableModel tableModel;
    private final JComboBox<String> reminderTypeCombo;
    private final JTextArea previewArea;
    private final RemindersDB remindersDB;
    private final TemplatesDB templatesDB;

    public RemindersPanel() {
        remindersDB  = new RemindersDB();
        templatesDB  = new TemplatesDB();

        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.add(new JLabel("Reminder Type:"));
        reminderTypeCombo = new JComboBox<>(new String[]{"1st Reminder", "2nd Reminder"});
        topPanel.add(reminderTypeCombo);

        JButton loadBtn    = new JButton("Load Accounts Due Reminder");
        JButton previewBtn = new JButton("Preview Reminder");
        JButton sendBtn    = new JButton("Mark as Sent");
        topPanel.add(loadBtn); topPanel.add(previewBtn); topPanel.add(sendBtn);
        add(topPanel, BorderLayout.NORTH);

        String[] cols = {"Customer ID","Name","Balance (£)","Account Status","Reminder Status","Pay By"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        overdueTable = new JTable(tableModel);

        previewArea = new JTextArea(10, 30);
        previewArea.setEditable(false);
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(overdueTable), new JScrollPane(previewArea));
        split.setDividerLocation(250);
        add(split, BorderLayout.CENTER);

        loadBtn.addActionListener(e -> loadOverdueAccounts());
        previewBtn.addActionListener(e -> previewReminder());
        sendBtn.addActionListener(e -> markAsSent());
    }

    private void loadOverdueAccounts() {
        String type = (String) reminderTypeCombo.getSelectedItem();
        tableModel.setRowCount(0);
        for (RemindersDB.ReminderRow row : remindersDB.loadOverdueAccounts(type)) {
            tableModel.addRow(new Object[]{
                    row.getCustomerId(), row.getCustomerName(),
                    String.format("%.2f", row.getBalance()),
                    row.getAccountStatus(), row.getReminderStatus(), row.getDueDate()
            });
        }
        previewArea.setText("");
    }

    private void previewReminder() {
        int row = overdueTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select an account first."); return; }

        String reminderType = (String) reminderTypeCombo.getSelectedItem();
        String customerId   = tableModel.getValueAt(row, 0).toString();
        String customerName = tableModel.getValueAt(row, 1).toString();
        String balance      = tableModel.getValueAt(row, 2).toString();
        String payBy        = tableModel.getValueAt(row, 5).toString();

        // Fetch template from DB and fill placeholders
        TemplatesDB.TemplateSettings settings = templatesDB.loadSettings();
        String template = settings.getReminderTemplate();

        String preview = template
                .replace("{customerName}",    customerName)
                .replace("{accountNo}",       customerId)
                .replace("{amount}",          "£" + balance)
                .replace("{balance}",         "£" + balance)
                .replace("{payByDate}",       payBy)
                .replace("{paymentDueDate}",  payBy)
                .replace("{pharmacyName}",    settings.getPharmacyName())
                .replace("{address}",         settings.getAddress())
                .replace("{email}",           settings.getEmail())
                .replace("{invoiceNo}",       "INV-" + customerId)
                .replace("{invoiceDate}",     java.time.LocalDate.now().toString())
                .replace("{firstReminderDate}", java.time.LocalDate.now().toString());

        // Prefix with 1st or 2nd reminder label
        String header = "1st Reminder".equals(reminderType) ? "REMINDER\n\n" : "SECOND REMINDER\n\n";
        previewArea.setText(header + preview);
    }

    private void markAsSent() {
        int row = overdueTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select an account first."); return; }

        String currentStatus = tableModel.getValueAt(row, 4).toString();
        if ("sent".equalsIgnoreCase(currentStatus)) {
            JOptionPane.showMessageDialog(this, "This reminder has already been sent.");
            return;
        }

        String customerId = tableModel.getValueAt(row, 0).toString();
        String name       = tableModel.getValueAt(row, 1).toString();
        String type       = (String) reminderTypeCombo.getSelectedItem();

        if (remindersDB.markReminderAsSent(customerId, type)) {
            tableModel.setValueAt("sent", row, 4);
            JOptionPane.showMessageDialog(this, "Reminder marked as sent for " + name + ".");
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update reminder status.");
        }
    }
}