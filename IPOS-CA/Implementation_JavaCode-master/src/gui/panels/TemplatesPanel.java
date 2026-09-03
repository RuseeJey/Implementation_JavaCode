package gui.panels;

import database.TemplatesDB;

import javax.swing.*;
import java.awt.*;

public class TemplatesPanel extends JPanel {

    private final JTextField pharmacyNameField;
    private final JTextField logoPathField;
    private final JTextField addressField;
    private final JTextField emailField;

    private final JTextArea reminderTemplateArea;
    private final JTextArea invoiceTemplateArea;
    private final TemplatesDB templatesDB;

    public TemplatesPanel() {
        templatesDB = new TemplatesDB();

        setLayout(new BorderLayout(10, 10));

        JPanel identityPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        identityPanel.setBorder(BorderFactory.createTitledBorder("Merchant Identity"));

        identityPanel.add(new JLabel("Pharmacy Name:"));
        pharmacyNameField = new JTextField("InfoPharma Pharmacy");
        identityPanel.add(pharmacyNameField);

        identityPanel.add(new JLabel("Logo Path:"));
        logoPathField = new JTextField("logo.png");
        identityPanel.add(logoPathField);

        identityPanel.add(new JLabel("Address:"));
        addressField = new JTextField("123 High Street, London");
        identityPanel.add(addressField);

        identityPanel.add(new JLabel("Email:"));
        emailField = new JTextField("contact@infopharma.co.uk");
        identityPanel.add(emailField);

        add(identityPanel, BorderLayout.NORTH);

        JTabbedPane templateTabs = new JTabbedPane();

        reminderTemplateArea = new JTextArea();
        reminderTemplateArea.setLineWrap(true);
        reminderTemplateArea.setWrapStyleWord(true);
        reminderTemplateArea.setText(
                "FIRST / SECOND REMINDER TEMPLATE\n\n" +
                        "Dear {customerName},\n\n" +
                        "Our records show an outstanding balance of £{balance}.\n" +
                        "Please make payment by {paymentDueDate}.\n\n" +
                        "Kind regards,\n" +
                        "{pharmacyName}"
        );

        invoiceTemplateArea = new JTextArea();
        invoiceTemplateArea.setLineWrap(true);
        invoiceTemplateArea.setWrapStyleWord(true);
        invoiceTemplateArea.setText(
                "RECEIPT / INVOICE TEMPLATE\n\n" +
                        "Receipt ID: {receiptId}\n" +
                        "Date: {date}\n" +
                        "Customer: {customerName}\n" +
                        "Total Paid: £{total}\n\n" +
                        "Thank you for your purchase.\n" +
                        "{pharmacyName}\n" +
                        "{address}\n" +
                        "{email}"
        );

        templateTabs.addTab("Reminder Template", new JScrollPane(reminderTemplateArea));
        templateTabs.addTab("Invoice Template", new JScrollPane(invoiceTemplateArea));

        add(templateTabs, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton previewButton = new JButton("Preview");
        JButton saveButton = new JButton("Save Templates");
        JButton resetButton = new JButton("Reset Default");

        buttonPanel.add(previewButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(resetButton);

        add(buttonPanel, BorderLayout.SOUTH);

        previewButton.addActionListener(e -> previewTemplate());
        saveButton.addActionListener(e -> saveTemplates());
        resetButton.addActionListener(e -> resetDefaults());

        loadTemplates();
    }

    private void loadTemplates() {
        TemplatesDB.TemplateSettings settings = templatesDB.loadSettings();

        pharmacyNameField.setText(settings.getPharmacyName());
        logoPathField.setText(settings.getLogoPath());
        addressField.setText(settings.getAddress());
        emailField.setText(settings.getEmail());
        reminderTemplateArea.setText(settings.getReminderTemplate());
        invoiceTemplateArea.setText(settings.getInvoiceTemplate());
    }

    private void previewTemplate() {
        String previewText =
                "Merchant Identity\n" +
                        "-------------------------\n" +
                        "Pharmacy Name: " + pharmacyNameField.getText().trim() + "\n" +
                        "Logo Path: " + logoPathField.getText().trim() + "\n" +
                        "Address: " + addressField.getText().trim() + "\n" +
                        "Email: " + emailField.getText().trim() + "\n\n" +
                        "Reminder Template Preview\n" +
                        "-------------------------\n" +
                        reminderTemplateArea.getText().trim() + "\n\n" +
                        "Invoice Template Preview\n" +
                        "-------------------------\n" +
                        invoiceTemplateArea.getText().trim();

        JTextArea previewArea = new JTextArea(previewText, 20, 50);
        previewArea.setEditable(false);
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);

        JOptionPane.showMessageDialog(
                this,
                new JScrollPane(previewArea),
                "Template Preview",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void saveTemplates() {
        if (pharmacyNameField.getText().trim().isEmpty()
                || addressField.getText().trim().isEmpty()
                || emailField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please complete the merchant identity fields.");
            return;
        }

        TemplatesDB.TemplateSettings settings = new TemplatesDB.TemplateSettings(
                pharmacyNameField.getText().trim(),
                logoPathField.getText().trim(),
                addressField.getText().trim(),
                emailField.getText().trim(),
                reminderTemplateArea.getText(),
                invoiceTemplateArea.getText()
        );

        boolean success = templatesDB.saveSettings(settings);
        if (!success) {
            JOptionPane.showMessageDialog(this, "Failed to save templates to database.");
            return;
        }

        JOptionPane.showMessageDialog(this, "Templates and merchant identity saved successfully.");
    }

    private void resetDefaults() {
        TemplatesDB.TemplateSettings defaults = templatesDB.defaultSettings();
        templatesDB.saveSettings(defaults);
        loadTemplates();
    }
}