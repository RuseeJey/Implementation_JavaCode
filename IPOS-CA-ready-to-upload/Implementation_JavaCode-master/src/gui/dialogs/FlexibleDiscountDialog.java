package gui.dialogs;

import javax.swing.*;
import java.awt.*;

public class FlexibleDiscountDialog extends JDialog {
    private final JTextField[] amountFields;
    private final JTextField[] discountFields;
    private boolean confirmed = false;

    public FlexibleDiscountDialog(JFrame parent) {
        super(parent, "Set Flexible Discount Tiers", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(550, 450);
        setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header
        JLabel headerLabel = new JLabel("Configure Flexible Discount Tiers");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 14));
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        // Info panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        JLabel infoLabel = new JLabel("<html>Set discount thresholds and rates.<br>" +
                "If subtotal &lt; Threshold 1: apply Discount 1<br>" +
                "If subtotal &lt; Threshold 2: apply Discount 2<br>" +
                "If subtotal &gt;= Threshold 2: apply Discount 3</html>");
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        infoPanel.add(infoLabel, BorderLayout.WEST);
        mainPanel.add(infoPanel, BorderLayout.NORTH);

        // Tier Configuration Panel
        JPanel tierPanel = new JPanel(new GridLayout(4, 4, 10, 10));
        tierPanel.setBorder(BorderFactory.createTitledBorder("Discount Tiers"));

        amountFields = new JTextField[3];
        discountFields = new JTextField[3];

        // Header row
        tierPanel.add(new JLabel("Tier"));
        tierPanel.add(new JLabel("Threshold (£)"));
        tierPanel.add(new JLabel("Description"));
        tierPanel.add(new JLabel("Discount (%)"));

        // Tier definitions
        String[] tierNames = {"Tier 1", "Tier 2", "Tier 3"};
        String[] tierLabels = {"Minimum amount", "From this amount", "Above this amount"};
        String[] tierHints = {"100", "300", "500"};
        String[] discountHints = {"0", "5", "10"};

        for (int i = 0; i < 3; i++) {
            // Amount field
            tierPanel.add(new JLabel(tierNames[i]));
            amountFields[i] = new JTextField(tierHints[i], 8);
            tierPanel.add(amountFields[i]);

            tierPanel.add(new JLabel(tierLabels[i]));

            discountFields[i] = new JTextField(discountHints[i], 8);
            tierPanel.add(discountFields[i]);
        }

        mainPanel.add(tierPanel, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");

        saveBtn.addActionListener(e -> onSave());
        cancelBtn.addActionListener(e -> onCancel());

        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void onSave() {
        try {
            // Validate inputs
            for (int i = 0; i < 3; i++) {
                Double.parseDouble(amountFields[i].getText().trim());
                Double.parseDouble(discountFields[i].getText().trim().replace("%", ""));
            }
            confirmed = true;
            dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for all fields.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String[] getTierAmounts() {
        String[] amounts = new String[3];
        for (int i = 0; i < 3; i++) {
            amounts[i] = amountFields[i].getText().trim();
        }
        return amounts;
    }

    public String[] getTierDiscounts() {
        String[] discounts = new String[3];
        for (int i = 0; i < 3; i++) {
            String val = discountFields[i].getText().trim().replace("%", "");
            discounts[i] = val;
        }
        return discounts;
    }

    public String getDiscountString() {
        String[] amounts = getTierAmounts();
        String[] discounts = getTierDiscounts();

        return "FLEX|" + normalizeThreshold(amounts[0]) + ":" + normalizeRate(discounts[0]) +
                "|" + normalizeThreshold(amounts[1]) + ":" + normalizeRate(discounts[1]) +
                "|" + normalizeThreshold(amounts[2]) + ":" + normalizeRate(discounts[2]);
    }

    private String normalizeThreshold(String value) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) {
            return "INF";
        }
        return v.toUpperCase();
    }

    private String normalizeRate(String value) {
        String v = value == null ? "" : value.trim().replace("%", "");
        if (v.isEmpty()) {
            return "0";
        }
        return v;
    }
}

